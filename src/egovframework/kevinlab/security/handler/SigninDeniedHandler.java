package egovframework.kevinlab.security.handler;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public class SigninDeniedHandler implements AccessDeniedHandler {
    private static final Logger logger = LoggerFactory.getLogger(SigninDeniedHandler.class);

    private String errorPage;

    public SigninDeniedHandler() {
    }

    public SigninDeniedHandler(String errorPage) {
        this.errorPage = errorPage;
    }

    public String getErrorPage() {
        return errorPage;
    }

    public void setErrorPage(String errorPage) {
        this.errorPage = errorPage;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException, ServletException {

        String error 	= "true";
        String message 	= exception.getMessage();

        System.out.println("DENIED : "+exception.toString());
        
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");

        String data = StringUtils.join(new String[] {
            " { \"response\" : {",
            " \"error\" : " , error , ", ",
            " \"message\" : \"", message , "\" ",
            "} } "
        });

        PrintWriter out = response.getWriter();
        out.print(data);
        out.flush();
        out.close();
    }
}