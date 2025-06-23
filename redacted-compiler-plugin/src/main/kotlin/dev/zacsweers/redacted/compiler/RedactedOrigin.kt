// Copyright (C) 2021 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.redacted.compiler

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl

internal val RedactedOrigin: IrDeclarationOrigin =
  IrDeclarationOriginImpl("GENERATED_REDACTED_CLASS_MEMBER")
