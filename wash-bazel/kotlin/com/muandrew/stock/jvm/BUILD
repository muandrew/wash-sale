load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "default",
    srcs = [
        "PreferredLot.kt",
        "ReleaseParser.kt",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//kotlin/com/muandrew/stock/model:default",
        "//kotlin/com/muandrew/stock/time:default",
        "@maven//:org_jsoup_jsoup",
    ],
)

kt_jvm_test(
    name = "test",
    srcs = ["ReleaseParserTest.kt"],
    associates = [":default"],
    data = glob([
        "testdata/*.*",
        "testdata/live/*.*",
    ]),
    test_class = "com.muandrew.stock.jvm.ReleaseParserTest",
    deps = [
        "//kotlin/com/muandrew/testtool",
        "@maven//:junit_junit",
    ],
)
