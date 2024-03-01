load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "adapters",
    srcs = [
        "LocalDateAdapter.kt",
        "LocalTimeAdapter.kt",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:com_squareup_moshi_moshi",
    ],
)
