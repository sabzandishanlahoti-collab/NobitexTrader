val context = androidx.compose.ui.platform.LocalContext.current

androidx.compose.runtime.LaunchedEffect(viewModel) {
    viewModel.applySettings(
        ir.nobitextrader.app.TraderSettingsStore.load(context)
    )
}
