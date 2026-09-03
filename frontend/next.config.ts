import type { NextConfig } from "next";

const standalone = process.env.DOCKER_BUILD === "1";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  ...(standalone ? { output: "standalone" as const } : {}),
};

export default nextConfig;
