package dev.zacsweers.redacted.compiler

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl

internal object RedactedOrigin : IrDeclarationOriginImpl("GENERATED_REDACTED_CLASS_MEMBER")
