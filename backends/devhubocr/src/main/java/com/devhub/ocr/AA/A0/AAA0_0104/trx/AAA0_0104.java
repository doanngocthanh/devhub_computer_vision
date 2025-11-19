package com.devhub.ocr.AA.A0.AAA0_0104.trx;

import com.devhub.ocr.AA.A0.AAA0_0104.mod.AAA0_0104Mod;
import com.devhub.ocr.app.systems.auth.UserObject;

import jakarta.servlet.http.HttpServletRequest;
 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/AA/A0/AAA0_0104")
public class AAA0_0104 {

    private final AAA0_0104Mod mod;

    public AAA0_0104(AAA0_0104Mod mod) {
        this.mod = mod;
    }

    @GetMapping("/")
    public String index(Model model, @ModelAttribute("currentUser") UserObject currentUser) {
        model.addAttribute("pageTitle", "Thông báo hệ thống");
        if (currentUser != null) {
            List<Map<String, Object>> notifications = mod.listForUser(currentUser.getId(), 50, 0);
            model.addAttribute("notifications", notifications);
        } else {
            model.addAttribute("notifications", java.util.Collections.emptyList());
        }
        return "html/AA/A0/AAA0_0104/AAA0_0104";
    }
   @GetMapping("/notify/json")
    @ResponseBody
    public List<Map<String, Object>> notifyJson(@RequestParam("userId") Long userId) {
        if (userId != null) {
            return mod.listForUser(userId, 50, 0);
        }
        return Collections.emptyList();
    }

    // @GetMapping("/notify/json")
    // @ResponseBody
    // public List<Map<String, Object>> notify(@ModelAttribute("currentUser") UserObject currentUser) {
    //     if (currentUser != null) {
    //         return mod.listForUser(currentUser.getId(), 50, 0);
    //     }
    //     return java.util.Collections.emptyList();
    // }

    // Mark a single delivery as read (convenience GET redirect)
    @GetMapping("/mark/read")
    public String markRead(@ModelAttribute("currentUser") UserObject currentUser,
                           @RequestParam("deliveryId") Long deliveryId,
                           HttpServletRequest req) {
        if (currentUser != null && deliveryId != null) {
            mod.markAsRead(deliveryId, currentUser.getId());
        }
        String referer = req.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/AA/A0/AAA0_0104/");
    }

    // Mark all as read
    @GetMapping("/mark/all")
    public String markAll(@ModelAttribute("currentUser") UserObject currentUser, HttpServletRequest req) {
        if (currentUser != null) {
            mod.markAllRead(currentUser.getId());
        }
        String referer = req.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/AA/A0/AAA0_0104/");
    }

    // Send a quick test notification to current user
    @GetMapping("/send/test")
    public String sendTest(@ModelAttribute("currentUser") UserObject currentUser, HttpServletRequest req) {
        if (currentUser != null) {
            mod.sendToUser(currentUser.getId(), "Thông báo thử", "Đây là thông báo test.");
        }
        String referer = req.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/AA/A0/AAA0_0104/");
    }

}
