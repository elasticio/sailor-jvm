package io.elastic.sailor

import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

public class SimpleRequestHandler extends AbstractHandler {
    public static String lastMessage = "";
    public static Map<String, String> headers = new HashMap<String, String>();

    private Deque<String> mockResponsePaths;

    public SimpleRequestHandler(String... mockResponsePath) {
        this.mockResponsePaths = new ArrayDeque<String>();

        for (int i = 0; i < mockResponsePath.length; i++) {
            this.mockResponsePaths.push(mockResponsePath[i]);
        }
    }

    @Override
    public void handle(String target, Request baseRequest,
                       HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        lastMessage = ""
        headers.clear();

        baseRequest.setHandled(true);
        response.setContentType("application/json");

        final InputStream body = request.getInputStream();

        pipe(body, System.err);
        pipe(body, response.getOutputStream());

        for(String headerName : request.getHeaderNames()){
            headers.put(headerName, request.getHeader(headerName));
        }

        System.err.println(headers)

        //pipe(this.getClass().getResourceAsStream(this.mockResponsePaths.pop()),response.getOutputStream());
    }

    public static void pipe(InputStream is, OutputStream os) throws IOException {
        int n;
        byte[] buffer = new byte[1];
        ByteArrayOutputStream message = new ByteArrayOutputStream()
        while ((n = is.read(buffer)) > -1) {
            os.write(buffer, 0, n);
            if (buffer[0] != null) {
                message.write(buffer)
            }
        }
        lastMessage += message.toString()
        message.close()
        os.close();
    }
}