import './globals.css'
import type { Metadata } from 'next'
import Script from 'next/script'
import AuthProvider from '@/components/AuthProvider'

export const metadata: Metadata = {
    title: 'Starto V2',
    description: 'Real-Time Ecosystem Intelligence Platform',
}

export default function RootLayout({
    children,
}: {
    children: React.ReactNode
}) {
    return (
        <html lang="en">
            <body>
                <AuthProvider>
                    {children}
                </AuthProvider>
                <Script 
                    src={`https://maps.googleapis.com/maps/api/js?key=${process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY}&libraries=places&loading=async&callback=Function.prototype`} 
                    strategy="afterInteractive" 
                />
            </body>
        </html>
    )
}
