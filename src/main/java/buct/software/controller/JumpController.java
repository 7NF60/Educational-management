package buct.software.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

/**
 * 这是一个通用页面跳转
 */
@Controller
public class JumpController {

    @RequestMapping(value = "/toPage",method = RequestMethod.GET)
    public String toPage(HttpServletRequest request){
        return request.getParameter("url");
    }
}