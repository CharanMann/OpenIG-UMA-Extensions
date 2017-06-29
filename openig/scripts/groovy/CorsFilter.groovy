import org.forgerock.http.protocol.Response
import org.forgerock.http.protocol.Status

if (request.method == 'OPTIONS') {
    /**
     * Supplies a response to a CORS preflight request.
     *
     * Example response:
     *
     * HTTP/1.1 200 OK
     * Access-Control-Allow-Origin: http://app.example.com:8081
     * Access-Control-Allow-Methods: POST
     * Access-Control-Allow-Headers: Authorization
     * Access-Control-Allow-Credentials: true
     * Access-Control-Max-Age: 3600
     */

    def origin = request.headers['Origin']?.firstValue
    def response = new Response(Status.OK)

    // Browsers sending a cross-origin request from a file might have Origin: null.
    response.headers.put("Access-Control-Allow-Origin", origin)
    request.headers['Access-Control-Request-Method']?.values.each() {
        response.headers.add("Access-Control-Allow-Methods", it)
    }
    request.headers['Access-Control-Request-Headers']?.values.each() {
        response.headers.add("Access-Control-Allow-Headers", it)
    }
    response.headers.put("Access-Control-Allow-Credentials", "true")
    response.headers.put("Access-Control-Max-Age", "3600")

    return response
}

return next.handle(context, request)
/**
 * Adds headers to a CORS response.
 */
        .thenOnResult({ response ->
    if (response.status.isServerError()) {
        // Skip headers if the response is a server error.
    } else {
        def headers = [
                "Access-Control-Allow-Origin": request.headers['Origin']?.firstValue,
                "Access-Control-Allow-Credentials": "true",
                "Access-Control-Expose-Headers": "WWW-Authenticate"
        ]
        response.headers.addAll(headers)
    }
})