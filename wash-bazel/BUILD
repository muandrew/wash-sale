load("@rules_kotlin//kotlin:core.bzl", "kt_compiler_plugin", "kt_ksp_plugin")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
#load("@gazelle//:def.bzl", "DEFAULT_LANGUAGES", "gazelle", "gazelle_binary")

# gazelle:prefix github.com/example/project
#gazelle(
#    name = "gazelle",
#    gazelle = ":gazelle_bin",
#)
#
#gazelle_binary(
#    name = "gazelle_bin",
#    languages = DEFAULT_LANGUAGES + [
#        "@contrib_rules_jvm//java/gazelle",
#    ],
#)

#kt_compiler_plugin(
#    name = "parcelize_plugin",
#    compile_phase = True,
#    id = "org.jetbrains.kotlin.parcelize",
#    stubs_phase = True,
#    deps = [
#        "@rules_kotlin//kotlin/compiler:parcelize-compiler-plugin",
#    ],
#)
#
#kt_jvm_library(
#    name = "parcelize",
#    srcs = [],
#    exported_compiler_plugins = [":parcelize_plugin"],
#    plugins = [
#        ":parcelize_plugin",
#    ],
#    visibility = ["//visibility:public"],
#    exports = [
#        "@rules_kotlin//kotlin/compiler:parcelize-runtime",
#    ],
#)

kt_ksp_plugin(
    name = "moshi-kotlin-codegen",
    processor_class = "com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessorProvider",
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
        "@maven//:com_squareup_moshi_moshi_kotlin_codegen",
    ],
)

exports_files(["MODULE.bazel.lock"])
