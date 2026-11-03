// Resolve WebAssembly and other emitted assets relative to the loaded bundle.
config.output = config.output || {};
config.output.publicPath = 'auto';

config.devServer = config.devServer || {};
config.devServer.devMiddleware = config.devServer.devMiddleware || {};
config.devServer.devMiddleware.publicPath = '/';
// Serve the app below any path without configuring a deployment prefix.
// Asset rewrites run before the HTML fallback so JavaScript and WebAssembly
// requests receive their actual files instead of index.html.
config.devServer.historyApiFallback = {
    rewrites: [
        {
            from: /^\/.+\/([^/]+\.(?:js|wasm|map))$/,
            to: context => '/' + context.match[1]
        }
    ]
};

// Allow reverse proxies / tunnel hosts (like Cloudflare Tunnel).
config.devServer.allowedHosts = 'all';
