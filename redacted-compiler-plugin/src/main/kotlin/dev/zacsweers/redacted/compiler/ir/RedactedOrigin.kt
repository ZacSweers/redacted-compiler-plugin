package dev.zacsweers.redacted.compiler.ir

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl

internal object RedactedOrigin : IrDeclarationOriginImpl("GENERATED_DATA_API_CLASS_MEMBER")