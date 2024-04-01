load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_library", "kt_jvm_test")
load("@rules_jvm_external//:kt_defs.bzl", "kt_jvm_export")

kt_jvm_library(
    name = "default",
    srcs = ["HtmlCli.kt"],
    deps = [
        "//kotlin/com/muandrew/moshi/adapters",
        "//kotlin/com/muandrew/stock/jvm:default",
        "//kotlin/com/muandrew/stock/world:default",
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_adapters",
        "@maven//:com_squareup_moshi_moshi_kotlin",
    ],
)

kt_jvm_binary(
    name = "cli",
    main_class = "com.muandrew.stock.jvm.cli.HtmlCli",
    runtime_deps = [
        ":default",
    ],
)

#kt_jvm_test(
#    name = "test",
#    srcs = ["HtmlCliTest.kt"],
#    data = glob(["testdata/*"]),
#    test_class = "com.muandrew.stock.jvm.cli.StockCliTest",
#    deps = [
#        ":default",
#        "//kotlin/com/muandrew/stock/model:default",
#        "//kotlin/com/muandrew/testtool",
#        "@maven//:com_squareup_moshi_moshi",
#        "@maven//:com_squareup_moshi_moshi_kotlin",
#        "@maven//:junit_junit",
#    ],
#)
