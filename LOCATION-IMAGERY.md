# Location imagery decision — October 25, 2026

Google Maps Embed is the best fit for a no-charge, interactive exterior reference inside the client workspace. It is not a downloadable property photograph and cannot guarantee coverage or a correct entrance. The application prepares this optional integration; no key, billing account, or external account was created.

| Option | Cost / setup | Fit |
| --- | --- | --- |
| Google Maps Embed, Street View mode | Unlimited requests at no charge; API key and Google Cloud setup required | Selected optional viewer. Uses saved latitude/longitude, looks within 50 metres, and loads only when the worker clicks Street View. |
| Google Street View Static | Usage-based pricing with a free usage allowance; key and billing setup | Better for still photographs, but not an unlimited-free service. Not enabled. |
| Mapillary | Free platform and API, application access token, visible attribution required | Useful contributed street imagery; nearest image may not show the customer's property. Not enabled. |

No provider is a perfect automatic photograph source for every address. Uploaded location photos remain the primary record. The demo does not present online photographs of real buildings as proof that fictional customers occupy them.

## Enable the optional viewer

1. Create/configure a Google Cloud project and enable **Maps Embed API**. Follow Google's current setup and billing requirements.
2. Create a browser API key restricted to **Maps Embed API only** and permitted HTTP referrers. Add your exact local origin and, for a demo, the specific Cloudflare hostname. Avoid unrestricted keys or broad Cloudflare wildcard referrers. This browser key is necessarily visible to signed-in staff.
3. Set `$env:GOOGLE_MAPS_EMBED_KEY='your-restricted-browser-key'` in the launcher shell and restart `run-local.ps1`. Do not commit the key.
4. Open a client and service location, choose **Set map position**, and enter verified entrance coordinates. Choose **Street View** to load the viewer.

The application does not geocode addresses, enable paid APIs, download Street View pictures, or store Google imagery. Coordinates are sent to Google only when opening the viewer. Google attribution stays in its embedded viewer. If coverage is missing, use site photos or the Directions link. The local preview tested the disabled configuration, coordinate validation, and access controls; live Google imagery requires your key and was not tested.

Sources:
- [Google Maps Embed usage and billing](https://developers.google.com/maps/documentation/embed/usage-and-billing)
- [Embed modes, Street View parameters and referrer restrictions](https://developers.google.com/maps/documentation/embed/embedding-map)
- [Google Street View Static usage and billing](https://developers.google.com/maps/documentation/streetview/usage-and-billing)
- [Mapillary FAQ](https://help.mapillary.com/hc/en-us/articles/8348198426396-Mapillary-FAQ)
- [Mapillary API examples and attribution](https://github.com/mapillary/api-demo)

## Equipment demo image sources

Manufacturer product images were downloaded for this local illustrative demo. Attribution links appear under each image; these are not photographs of the fictional installation. Manufacturer copyrights remain with their owners; this does not assert a general redistribution license.

- Carrier: [59TP6 family product page](https://www.carrier.com/residential/en/ca/products/furnaces/59tp6/) — `demo-images/furnace.png`; current family illustration, not an exact revision photograph.
- Delta: [Leland 9178-AR-DST](https://www.deltafaucet.com/kitchen/product/9178-AR-DST.html) — `demo-images/faucet.jpg`.
- Bosch: [SHPM65Z55N/24 support page](https://www.bosch-home.com/us/en/productservice/SHPM65Z55N-24) — `demo-images/dishwasher.jpg`, resized from that page's product illustration. Confirm the appliance's full model suffix for manuals.

The Delta installation tool example is drawn from page 4 of [106557 Rev H](https://media.deltafaucet.com/MandI/106557%20Rev%20H.pdf), specifically mounting hardware instructions. It is a scoped, partial preparation list, not an assertion that two tools suffice for every installation.
