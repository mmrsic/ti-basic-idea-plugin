package com.github.mmrsic.idea.plugins.tibasic.ext

import com.intellij.psi.PsiElement

inline fun <reified T : PsiElement> PsiElement.firstChildOfType(): T? =
    children.filterIsInstance<T>().firstOrNull()
