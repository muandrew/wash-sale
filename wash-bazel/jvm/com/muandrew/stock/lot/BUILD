load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "lot",
    srcs = ["ShareValue.kt"],
    visibility = ["//visibility:public"],
    deps = [
        "//jvm/com/muandrew/money",
    ],
)

kt_jvm_test(
    name = "test",
    srcs = ["ShareValueTest.kt"],
    test_class = "com.muandrew.stock.lot.ShareValueTest",
    deps = [
        ":lot",
        "@maven//:junit_junit",
    ],
)
