package org.pdi.core.kernels

import org.pdi.core.LinearKernel

// src: https://en.wikipedia.org/wiki/Roberts_cross
class RobertsXKernel : LinearKernel(2, 2) {
    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(false,false)

    override fun generateKernel() {
        kernel = arrayOf(
            floatArrayOf(1f, 0f),
            floatArrayOf(0f, -1f)
        )
    }
}

class RobertsYKernel : LinearKernel(2, 2) {
    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(false,false)

    override fun generateKernel() {
        kernel = arrayOf(
            floatArrayOf(0f, 1f),
            floatArrayOf(-1f, 0f)
        )
    }
}