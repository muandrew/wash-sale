load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "stock",
    srcs = [
        "Lot.kt",
        "LotIdentifier.kt",
        "LotSnapshot.kt",
        "MoshiExt.kt",
        "Transaction.kt",
        "TransactionId.kt",
        "World.kt",
    ],
    plugins = [
        "//:moshi-kotlin-codegen",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//jvm/com/muandrew/money",
        "//jvm/com/muandrew/moshi/adapters",
        "//jvm/com/muandrew/stock/time",
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_adapters",
    ],
)

kt_jvm_test(
    name = "test",
    srcs = ["WorldTest.kt"],
    data = [
        "testdata/1.json",
        "testdata/2.json",
    ],
    test_class = "com.muandrew.stock.WorldTest",
    deps = [
        ":stock",
        "//jvm/com/muandrew/testtool",
        "@bazel_tools//tools/java/runfiles",
        "@maven//:com_squareup_moshi_moshi_kotlin",
        "@maven//:junit_junit",
    ],
)
