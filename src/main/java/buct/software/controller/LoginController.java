package buct.software.controller;

import buct.software.domain.Student;
import buct.software.domain.Teacher;
import buct.software.domain.User;
import buct.software.domain.norm.Constant;
import buct.software.service.*;
import buct.software.utils.UserAgentParser;
import buct.software.views.SelectCourseView;
import buct.software.views.StudentGradeIndexView;

import buct.software.views.TeaCourseView;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import sun.security.provider.MD5;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

import java.util.Map;

/**
* @Author WangBeiBei
* @description 登录控制
*/
@Controller
public class LoginController {
    final
    UserService userService;
    final
    StudentService studentService;
    final
    TeacherService teacherService;
    final
    SelectCourseService selectCourseService;
    final
    SemesterService semesterService;
    final
    SchedulingService schedulingService;
    final
    CollegeService collegeService;
    final
    Environment environment;

    public LoginController(UserService userService, StudentService studentService, TeacherService teacherService, SelectCourseService selectCourseService, SemesterService semesterService, SchedulingService schedulingService, CollegeService collegeService, Environment environment) {
        this.userService = userService;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.selectCourseService = selectCourseService;
        this.semesterService = semesterService;
        this.schedulingService = schedulingService;
        this.collegeService = collegeService;
        this.environment = environment;
    }

    /**
     * 登录页面网址，请求这个地址用于展现登录页面
     * 当然，如果已经有登陆信息的话，会直接跳转到登录成功的界面。
     *
     * @param request 这里是 一个HttpServletRequest 用于获取 session 相关信息。
     * @return
     */
    @RequestMapping("/index")
    public String index(HttpServletRequest request, Map<String, Object> parmMap) {
        HttpSession session = request.getSession();
        Object userInfo = session.getAttribute("user");
        Integer semesterId = semesterService.getCurrentSemesterId();

        /*
         * 获取当前的用户使用的是什么设备。
         */
        String useragent = request.getHeader("User-Agent");
        UserAgentParser userAgentParser = new UserAgentParser(useragent);
        String platform = userAgentParser.getPlatform();

        if (userInfo == null) {
            if (platform.equals("mobile"))
                return "MobileLogin";
            else
                return "login";

        } else {
            User user = (User) userInfo;
            Integer type = user.getType();
            parmMap.put("userinfo", user);
            if (type == 0) {
                List<SelectCourseView> courseTable = selectCourseService.getCourseTable(semesterId, user.getAccount());
                List<StudentGradeIndexView> gradeList = selectCourseService.getGrade(semesterId, user.getAccount());
                parmMap.put("courseTable", courseTable);
                parmMap.put("gradeList", gradeList);

                if (platform.equals("mobile"))
                    return "msh";

                return "student";
            }
            if (type == 1) {
                int tno = user.getAccount();
                List<TeaCourseView> teaCourseViews = schedulingService.getCourseInfoByTno(tno);
                session.setAttribute("CourseTable", teaCourseViews);

                parmMap.put("courseTable", teaCourseViews);//课表
                Teacher teacher = teacherService.getTeacherByTno(tno);
                parmMap.put("teainfo", teacher);
                int cid = teacher.getCollegeId();
                String colname = collegeService.getColnameById(cid);
                parmMap.put("colname", colname);
                if (platform.equals("mobile"))
                    return "MobileTeacherHome";
                return "teacher";
            }
            if (type == 2) {
                if (platform.equals("mobile")) {
                    return "redirect:/GoMobileHomePage";
                } else {
                    return "redirect:/GoHomePage";
                }
            }
            return "login";
        }
    }

    /**
     * 这是点击登录按钮之后的处理函数: 查数据库，匹配密码等。
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String login(HttpServletRequest request,
                        Map<String, Object> paraMap,
                        @RequestParam("account") String account,
                        @RequestParam("password") String password) {


        /*
         * 获取当前的用户使用的是什么设备。
         */
        String useragent = request.getHeader("User-Agent");
        UserAgentParser userAgentParser = new UserAgentParser(useragent);
        String platform = userAgentParser.getPlatform();


        HttpSession session = request.getSession();
        User user = userService.LoginFun(account, DigestUtils.md5DigestAsHex((environment.getProperty("password.salt") + password).getBytes()));
        if (user == null) {
            paraMap.put("error_msg", "用户名或者密码错误，请重新输入");
            if (platform.equals("mobile"))
                return "MobileLogin";

            return "login";
        } else {
            // 查询详细保存在session 中，也就是说登录的是一个学生的话，
            // 还要保存学生的信息，如果是一个老师，要保存一个老师的信息
            if (!Constant.normal.equals(user.getStatus())) {
                paraMap.put("error_msg", "用户已冻结，请联系教务管理员");
                return platform.equals("mobile")?"MobileLogin":"login";
            }
            boolean error = false;
            if (user.getType() == 0) {
                // 是一个学生
                Student student = studentService.getStudentBySno(user.getAccount());
                if (student != null) {
                    user.setMajor(student.getMajor());
                    user.setSname(student.getSname());
                    user.setMajorid(student.getMajorId());
                    user.setCollege(student.getCollege());
                    user.setKlass(student.getKlass());

                } else error = true;
            } else if (user.getType() == 1) {
                // 是一个老师
                Teacher teacher = teacherService.getTeacherByTno(user.getAccount());
                if (teacher != null) {
                    user.setTname(teacher.getTname());
                } else {
                    error = true;
                }
            } else if (user.getType() == 2) {
                // 这是管理员
                Teacher teacher = teacherService.getTeacherByTno(user.getAccount());
                if (teacher != null) {
                    user.setTname(teacher.getTname());
                } else {
                    error = true;
                }
            }
            if (error) {
                paraMap.put("error_msg", "数据库中找不到您的详细信息，请联系管理员");

                if (platform.equals("mobile"))
                    return "MobileLogin";

                return "login";
            } else {
                session.setAttribute("user", user);
                return "redirect:/index";
            }
        }
    }

    @RequestMapping(value = "/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.setAttribute("user", null);
        return "redirect:/index";
    }
}