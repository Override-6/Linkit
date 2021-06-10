package fr.linkit.api.local.generation

import java.io.{InputStream, OutputStream}


trait CompilationRequest {

    def sourceCode: String

    def compilerInput: InputStream

    def compilerOutput: OutputStream
    
    def compilerErrOutput: OutputStream
}
