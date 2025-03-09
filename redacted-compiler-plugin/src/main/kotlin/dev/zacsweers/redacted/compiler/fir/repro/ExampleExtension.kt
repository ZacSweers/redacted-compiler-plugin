package dev.zacsweers.redacted.compiler.fir.repro

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate.BuilderContext.parentAnnotated
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverriddenFromAny
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private val redactedTypeFqName = FqName("dev.zacsweers.redacted.RedactedType")
private val redactedTypeFactoryFqName = FqName("dev.zacsweers.redacted.RedactedType.Factory")

/**
 * A supertype generator that attempts to generate a supertype onto a companion object of a
 * `@RedactedType` class such that the below code:
 * ```
 * @RedactedType
 * class Example(val redactedString: String) {
 *   @RedactedType.Factory
 *   interface Factory {
 *     fun create(redactedString: String)
 *   }
 *
 *   // Existing companion object
 *   companion object
 * }
 * ```
 *
 * Is transformed to
 *
 * ```
 * @RedactedType
 * class Example(val redactedString: String) {
 *   @RedactedType.Factory
 *   interface Factory {
 *     fun create(redactedString: String)
 *   }
 *
 *   // Existing companion object now implements Factory
 *   companion object : Factory
 * }
 * ```
 *
 * However, while this does run, it never reaches IR because FIR does not generate a fake override
 * for the new inherited `create()` function.
 */
internal class SupertypeGeneratorExtension(session: FirSession) :
  FirSupertypeGenerationExtension(session) {

  private val predicate by lazy { parentAnnotated(redactedTypeFqName) }

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(predicate)
  }

  override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
    // Assumes the existence of a `@RedactedType.Factory`-annotated nested class in the parent
    return declaration.symbol.isCompanion
  }

  override fun computeAdditionalSupertypes(
    classLikeDeclaration: FirClassLikeDeclaration,
    resolvedSupertypes: List<FirResolvedTypeRef>,
    typeResolver: TypeResolveService,
  ): List<ConeKotlinType> {
    val redactedType = classLikeDeclaration.getContainingDeclaration(session) ?: return emptyList()
    if (redactedType !is FirClass) return emptyList()
    val factoryType =
      redactedType.declarations.filterIsInstance<FirClass>().firstOrNull {
        // Superficial check. In a real one we would check the resolved annotation
        it.nameOrSpecialName == Name.identifier("Factory")
      } ?: return emptyList()

    return listOf(factoryType.defaultType())
  }
}

internal class CompanionFactoryTransformer(private val pluginContext: IrPluginContext) :
  IrElementTransformerVoid() {
  private val stdlibErrorFunction: IrFunctionSymbol by lazy {
    pluginContext.referenceFunctions(CallableId(FqName("kotlin"), Name.identifier("error"))).first()
  }

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  override fun visitClass(declaration: IrClass): IrStatement {
    if (declaration.isCompanion && declaration.parentAsClass.hasAnnotation(redactedTypeFqName)) {
      val functionToImplement =
        declaration.functions.single { it.isFakeOverride && !it.isFakeOverriddenFromAny() }

      functionToImplement.apply {
        isFakeOverride = false
        modality = Modality.FINAL
        body =
          pluginContext.createIrBuilder(symbol).run {
            irExprBody(
              irCall(callee = stdlibErrorFunction).apply { putValueArgument(0, irString("Stub")) }
            )
          }
      }
    }
    return super.visitClass(declaration)
  }

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  private fun IrGeneratorContext.createIrBuilder(symbol: IrSymbol): DeclarationIrBuilder {
    return DeclarationIrBuilder(this, symbol, symbol.owner.startOffset, symbol.owner.endOffset)
  }
}
