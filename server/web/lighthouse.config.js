{
  "extends": "lighthouse:default",
  "settings": {
    "onlyCategories": ["performance", "accessibility", "best-practices", "seo"],
    "output": ["html", "json"],
    "outputPath": "./lighthouse-report",
    "formFactor": "desktop",
    "throttling": {
      "rttMs": 40,
      "throughputKbps": 10240,
      "cpuSlowdownMultiplier": 1
    },
    "screenEmulation": {
      "mobile": false,
      "width": 1920,
      "height": 1080,
      "deviceScaleFactor": 1,
      "disabled": false
    }
  },
  "audits": [
    "first-contentful-paint",
    "largest-contentful-paint",
    "first-meaningful-paint",
    "speed-index",
    "total-blocking-time",
    "cumulative-layout-shift",
    "interactive",
    "render-blocking-resources",
    "unused-css-rules",
    "unused-javascript",
    "uses-optimized-images",
    "modern-image-formats",
    "offscreen-images",
    "defer-offscreen-images",
    "unminified-css",
    "unminified-javascript",
    "unused-css",
    "unused-javascript",
    "uses-text-compression",
    "uses-responsive-images",
    "server-response-time",
    "redirects",
    "bootup-time",
    "mainthread-work-breakdown",
    "dom-size",
    "critical-request-chains",
    "resource-summary",
    "third-party-summary",
    "third-party-facades"
  ]
}
