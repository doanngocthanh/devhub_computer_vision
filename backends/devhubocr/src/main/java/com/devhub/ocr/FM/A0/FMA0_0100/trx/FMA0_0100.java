package com.devhub.ocr.FM.A0.FMA0_0100.trx;

import com.devhub.ocr.app.systems.auth.AuthContext;
import com.devhub.ocr.app.systems.auth.UserObject;
import com.devhub.ocr.app.systems.menu.AutoMenu;
import com.devhub.ocr.app.systems.mod.FileService;
import com.devhub.ocr.FM.A0.FMA0_0100.mod.FMA0_0100Mod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/FM/A0/FMA0_0100")
@AutoMenu(title = "Chỉnh sửa hồ sơ", icon = "user", path = "/FM/A0/FMA0_0100/", roles = {})
public class FMA0_0100 {

    private final FMA0_0100Mod mod;
    private final FileService fileService;

    public FMA0_0100(FMA0_0100Mod mod, FileService fileService) {
        this.mod = mod;
        this.fileService = fileService;
    }

    @GetMapping({"", "/"})
    public String index(Model model) {
        UserObject u = AuthContext.get();
        if (u == null) return "redirect:/auth/sign-in";

        // ensure avatar column exists and load user row via mod
        mod.ensureAvatarColumn();
        Map<String, Object> row = mod.getUserById(u.getId());
        Map<String, Object> profile = mod.getProfile(u.getId());
        model.addAttribute("profile", profile);
        String avatar = null;
        if (row != null && row.get("avatar") != null) avatar = String.valueOf(row.get("avatar"));
        String avatarUrl = avatar == null || avatar.isBlank() ? "" : fileService.getPublicUrl(avatar);

        model.addAttribute("pageTitle", "Chỉnh sửa hồ sơ");
        model.addAttribute("user", u);
        model.addAttribute("avatarUrl", avatarUrl);
        return "html/FM/A0/FMA0_0100/FMA0_0100";
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam(required = false) String firstName,
                                @RequestParam(required = false) String lastName,
                                @RequestParam(required = false) String businessName,
                                @RequestParam(required = false) String businessId,
                                @RequestParam(required = false) String location,
                                @RequestParam(required = false) String publicProfile,
                                @RequestParam(required = false) MultipartFile avatarFile,
                                Model model) {
        UserObject u = AuthContext.get();
        if (u == null) return "redirect:/auth/sign-in";

        try {
            mod.ensureAvatarColumn();

            String avatarFilename = null;
            if (avatarFile != null && !avatarFile.isEmpty()) {
                // derive extension
                String original = avatarFile.getOriginalFilename();
                String ext = "";
                if (original != null && original.contains(".")) ext = original.substring(original.lastIndexOf('.'));
                String target = "user-" + u.getId() + "-avatar" + ext;
                avatarFilename = fileService.saveUploadedFile(avatarFile, target);
            }

            // update via mod (includes business/profile fields)
            boolean ok = mod.updateProfile(u.getId(), firstName, lastName, businessName, businessId, location, publicProfile != null, avatarFilename);

            // optional: handle failure - populate model and return template so Thymeleaf has expected attributes
            if (!ok) {
                model.addAttribute("error", "Không thể cập nhật hồ sơ");
                Map<String, Object> profile = mod.getProfile(u.getId());
                model.addAttribute("profile", profile);
                Map<String, Object> row = mod.getUserById(u.getId());
                String avatar = null;
                if (row != null && row.get("avatar") != null) avatar = String.valueOf(row.get("avatar"));
                String avatarUrl = avatar == null || avatar.isBlank() ? "" : fileService.getPublicUrl(avatar);
                model.addAttribute("avatarUrl", avatarUrl);
                model.addAttribute("user", u);
                model.addAttribute("pageTitle", "Chỉnh sửa hồ sơ");
                return "html/FM/A0/FMA0_0100/FMA0_0100";
            }

            // refresh AuthContext user display fields
            if (firstName != null) u.setFirstName(firstName);
            if (lastName != null) u.setLastName(lastName);

        } catch (IOException ex) {
            model.addAttribute("error", "Không thể lưu avatar: " + ex.getMessage());
            // populate model same as GET so template can render safely
            Map<String, Object> profile = mod.getProfile(u.getId());
            model.addAttribute("profile", profile);
            Map<String, Object> row = mod.getUserById(u.getId());
            String avatar = null;
            if (row != null && row.get("avatar") != null) avatar = String.valueOf(row.get("avatar"));
            String avatarUrl = avatar == null || avatar.isBlank() ? "" : fileService.getPublicUrl(avatar);
            model.addAttribute("avatarUrl", avatarUrl);
            model.addAttribute("user", u);
            model.addAttribute("pageTitle", "Chỉnh sửa hồ sơ");
            return "html/FM/A0/FMA0_0100/FMA0_0100";
        }

        return "redirect:/FM/A0/FMA0_0100/?updated=1";
    }

}
