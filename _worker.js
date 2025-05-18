export default {
  async fetch(request, env) {
    // 检查请求路径是否存在对应的文件
    const asset = await env.ASSETS.fetch(request);
    if (asset.status !== 404) {
      return asset;
    }

    // 不存在则返回404
    return new Response("Not Found", { status: 404 });
  }
};