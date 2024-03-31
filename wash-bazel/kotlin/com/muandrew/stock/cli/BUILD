load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_test")
load("@rules_jvm_external//:kt_defs.bzl", "kt_jvm_export")

kt_jvm_export(
    name = "lib",
    srcs = ["StockCli.kt"],
    maven_coordinates = "com.muandrew.stock:cli:0.0.1",
    deps = [
        "//kotlin/com/muandrew/moshi/adapters",
        "//kotlin/com/muandrew/stock/world:default",
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_adapters",
        "@maven//:com_squareup_moshi_moshi_kotlin",
    ],
)

kt_jvm_binary(
    name = "cli",
    main_class = "com.muandrew.stock.cli.StockCli",
    runtime_deps = [
        ":lib-lib",
        "//kotlin/com/muandrew/moshi/adapters",
        "//kotlin/com/muandrew/stock/world:default",
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_adapters",
        "@maven//:com_squareup_moshi_moshi_kotlin",
    ],
)

kt_jvm_test(
    name = "test",
    srcs = ["StockCliTest.kt"],
    data = glob(["testdata/*"]),
    test_class = "com.muandrew.stock.cli.StockCliTest",
    deps = [
        ":lib-lib",  # macro expands the lib target to :%s-lib
        "//kotlin/com/muandrew/stock/model:default",
        "//kotlin/com/muandrew/testtool",
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
        "@maven//:junit_junit",
    ],
)
