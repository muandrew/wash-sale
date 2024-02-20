load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "money",
    srcs = ["Money.kt"],
    visibility = ["//visibility:public"],
)

kt_jvm_test(
    name = "test",
    srcs = ["MoneyTest.kt"],
    test_class = "com.muandrew.money.MoneyTest",
    deps = [
        ":money",
        "@maven//:junit_junit",
    ],
)
