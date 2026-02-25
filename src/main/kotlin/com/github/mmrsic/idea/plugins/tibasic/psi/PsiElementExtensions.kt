package com.github.mmrsic.idea.plugins.tibasic.psi

import com.intellij.psi.PsiElement

val PsiElement.containingTiBasicFile: TiBasicFile? get() = containingFile as? TiBasicFile
