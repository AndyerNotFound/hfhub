package com.hfhub.android

/**
 * 沙箱类型检查用的 R 替身(真机由 AGP 从资源生成, 不进 APK)。
 * 只声明代码里实际引用的符号, 与 res/ 下资源一一对应。
 */
object R {
    object id {
        const val root = 0x7f010001
        const val pager = 0x7f010002
        const val mainContent = 0x7f010003
        const val secondaryContainer = 0x7f010004
        const val toolbar = 0x7f010005
        const val bottomNav = 0x7f010006
        const val nav_models = 0x7f010007
        const val nav_datasets = 0x7f010008
        const val nav_spaces = 0x7f010009
        const val nav_papers = 0x7f01000a
        const val action_source = 0x7f01000b
        const val action_copy = 0x7f01000c
        const val action_web = 0x7f01000d
    }

    object layout {
        const val activity_main = 0x7f020001
    }

    object menu {
        const val bottom_nav_main = 0x7f080001
        const val toolbar_main = 0x7f080002
        const val detail_menu = 0x7f080003
    }

    object string {
        const val app_name = 0x7f030001
        const val nav_models = 0x7f030002
        const val nav_datasets = 0x7f030003
        const val nav_spaces = 0x7f030004
        const val nav_papers = 0x7f030005
        const val menu_source = 0x7f030006
        const val detail_copy = 0x7f030007
        const val detail_web = 0x7f030008
    }

    object attr {
        const val colorPrimary = 0x7f040001
        const val colorPrimaryContainer = 0x7f040002
        const val colorOnPrimaryContainer = 0x7f040003
        const val colorSecondaryContainer = 0x7f040004
        const val colorOnSecondaryContainer = 0x7f040005
        const val colorSurface = 0x7f040006
        const val colorOnSurface = 0x7f040007
        const val colorOnSurfaceVariant = 0x7f040008
        const val colorSurfaceContainerHigh = 0x7f040009
        const val colorSurfaceContainerLow = 0x7f04000a
    }

    object color {
        const val ic_launcher_background = 0x7f050001
    }

    object drawable {
        const val ic_launcher_foreground = 0x7f060001
        const val ic_search = 0x7f060002
        const val ic_nav_models = 0x7f060003
        const val ic_nav_datasets = 0x7f060004
        const val ic_nav_spaces = 0x7f060005
        const val ic_nav_papers = 0x7f060006
        const val ic_action_settings = 0x7f060007
        const val ic_arrow_back = 0x7f060008
        const val ic_action_download = 0x7f060009
        const val ic_folder = 0x7f06000a
        const val ic_file = 0x7f06000b
        const val ic_filter = 0x7f06000c
        const val ic_close = 0x7f06000d
    }

    object anim {
        const val slide_in_right = 0x7f090001
        const val slide_out_right = 0x7f090002
        const val fade_in = 0x7f090003
        const val fade_out = 0x7f090004
    }

    object mipmap {
        const val ic_launcher = 0x7f070001
    }
}
