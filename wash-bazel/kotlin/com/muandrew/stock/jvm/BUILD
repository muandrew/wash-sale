load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "default",
    srcs = [
        "PartialWithdarawData.kt",
        "PreferredLot.kt",
        "RealtimeTransaction.kt",
        "StatementParser.kt",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//kotlin/com/muandrew/csv:default",
        "//kotlin/com/muandrew/stock/model:default",
        "//kotlin/com/muandrew/stock/time:default",
        "@maven//:org_jsoup_jsoup",
    ],
)

kt_jvm_test(
    name = "test",
    srcs = ["StatementParserTest.kt"],
    associates = [":default"],
    data = glob([
        "testdata/*.*",
        "testdata/live/*.*",
    ]),
    test_class = "com.muandrew.stock.jvm.StatementParserTest",
    deps = [
        "//kotlin/com/muandrew/testtool",
        "@maven//:junit_junit",
    ],
)
