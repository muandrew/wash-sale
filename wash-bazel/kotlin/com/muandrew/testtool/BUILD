load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "testtool",
    srcs = ["TestFile.kt"],
    visibility = ["//visibility:public"],
    deps = [
        "@bazel_tools//tools/java/runfiles",
    ],
)
