/** @type {import('next').NextConfig} */
const nextConfig = {
    output: 'standalone',
    images: {
        domains: ['api.dicebear.com'],
        dangerouslyAllowSVG: true,
        contentSecurityPolicy: "default-src 'self'; script-src 'none'; sandbox;",
    },
    // Allow backend API calls during SSR within Docker network
    async rewrites() {
        return process.env.NODE_ENV === 'production' ? [] : [
            {
                source: '/api/:path*',
                destination: `${process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'}/api/:path*`,
            },
        ];
    },
};

module.exports = nextConfig;
