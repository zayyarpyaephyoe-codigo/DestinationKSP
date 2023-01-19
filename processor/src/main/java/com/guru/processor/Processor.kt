package com.guru.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.validate
import com.guru.annonation.EncodeUrl
import com.guru.annonation.Destination
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


//RezeveDestination annonation processing should complete in first compilation round
private val FIRST_ROUND_ONLY = emptyList<KSAnnotated>()

class Processor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    @OptIn(KotlinPoetKspPreview::class, KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray())

        val symbolAnnonation = resolver.getSymbolsWithAnnotation(
            Destination::class.qualifiedName.toString()
        )
        val symbolClasses = symbolAnnonation.filterIsInstance(KSClassDeclaration::class.java)

        symbolClasses.forEach { classes ->
            classes.annotations.firstOrNull() {
                it.shortName.asString() == Destination::class.simpleName
            }?.let {
                val screenName = it.arguments.first().value.toString()
                FileSpec.builder(
                    packageName = classes.packageName.asString(),
                    fileName = classes.simpleName.asString() + "Destination"
                ).apply {
                    addType(
                        TypeSpec.objectBuilder(
                            classes.simpleName.asString() + "Destination"
                        ).addFunction(
                            FunSpec.builder("destination")
                                .returns(STRING)
                                .addStatement(
                                    "return \"%L\"",
                                    createRouteNameKey(
                                        screenName,
                                        classes.getAllProperties().map {
                                            it.simpleName.asString()
                                        }.toList()
                                    )
                                )
                                .build()
                        ).addFunction(
                            FunSpec.builder("route")
                                .returns(STRING)
                                .addParameters(
                                    classes.getAllProperties().filter {
                                        it.extensionReceiver == null
                                    }.map {
                                        ParameterSpec.builder(
                                            it.simpleName.asString(),
                                            it.type.resolve().toClassName()
                                        )
                                            .build()
                                    }.toList()
                                ).addStatement(
                                    "return \"%L\"",
                                    createArgsRoute(
                                        screenName,
                                        classes.getAllProperties().map {
                                            val encodeUrl = it.annotations.firstOrNull {
                                                it.shortName.asString() == EncodeUrl::class.simpleName
                                            }
                                            if (encodeUrl == null) {
                                                //check it has @EncodeUrl
                                                it.simpleName.asString()
                                            } else {
                                                it.simpleName.asString() + ".encodeUrl()"
                                            }
                                        }.toList()
                                    )
                                )
                                .build()
                        ).addFunctions(
                            classes.getAllProperties().map { prop ->
                                FunSpec.builder(
                                    name = prop.simpleName.asString()
                                ).returns(
                                    prop.type.resolve()
                                        .toClassName().copy(nullable = true)
                                ).addParameter(
                                    ParameterSpec.builder(
                                        "bundle",
                                        ClassName("android.os", "Bundle")
                                    )
                                        .build()

                                ).addCode(
                                    """
                                        return ${
                                        getBundleValueByReferenceType(
                                            prop.type, prop.simpleName.asString()
                                        ).orEmpty()
                                    }
                                    """.trimIndent()
                                )
                                    .build()

                            }.toList()
                        ).addFunction(
                            FunSpec.builder("encodeUrl")
                                .addModifiers(KModifier.PRIVATE)
                                .returns(STRING)
                                .receiver(STRING)
                                /*
                                *
                                * private fun String.encodeUrl(): String {
    return if (this.isNotBlank()) {
        URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
    } else ""
}
                                * */
                                .beginControlFlow("if(%L)", "this == \"\"")
                                .addStatement("return \"\"")
                                .endControlFlow()
                                .addStatement(
                                    "return %T.encode(this , %T.UTF_8.toString())",
                                    URLEncoder::class, StandardCharsets::class
                                )
                                .build()
                        )
                            .addProperties(
                                classes.getAllProperties().map {
                                    PropertySpec.builder(
                                        it.simpleName.asString().uppercase(),
                                        STRING
                                    )
                                        .initializer("%S", it.simpleName.asString())
                                        .build()
                                }.toList()
                            )
                            .build()
                    )
                }
                    .build().writeTo(
                        codeGenerator = codeGenerator,
                        dependencies = dependencies
                    )

            }
        }

        return symbolAnnonation.filterNot {
            it.validate()
        }.toList()
    }
}

@OptIn(KotlinPoetKspPreview::class)
private fun getBundleValueByReferenceType(type: KSTypeReference, key: String): String? {
    val typeName = type.resolve().toClassName().simpleName
    return if ("String" == typeName)
        "bundle.getString(\"$key\")"
    else if ("Boolean" == typeName)
        "bundle.getBoolean(\"$key\")"
    else if ("Int" == typeName) "bundle.getInt(\"$key\")"
    else if ("Float" == typeName) "bundle.getFloat(\"$key\")"
    else if ("Double" == typeName) "bundle.getDouble(\"$key\")"
    else if ("Long" == typeName) "bundle.getLong(\"$key\")"
    else
        return null
}

private fun createArgsRoute(screen: String, args: List<String>): String {
    val formattedArgs = args.joinToString(
        separator = "/"
    ) { "$" + "{" + it + "}" }
    return "$screen/$formattedArgs"
}

private fun createRouteNameKey(screen: String, key: List<String>): String {
    val formattedKeys = key.joinToString(
        separator = "/"
    ) { "{$it}" }
    return "$screen/$formattedKeys"
}



