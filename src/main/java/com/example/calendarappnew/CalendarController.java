package com.example.calendarappnew;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
public class CalendarController {

    // Spring Securityのユーザー管理システムをここに直結させます
    public static final InMemoryUserDetailsManager userManager = new InMemoryUserDetailsManager();

    static {
        // 初期ユーザー（user / pass）をあらかじめ登録
        UserDetails defaultUser = User.withUsername("user")
                .password("{noop}pass")
                .roles("USER")
                .build();
        userManager.createUser(defaultUser);
    }

    // 1. ログイン画面（ユーザー登録エラーの注意メッセージ対応）
    @GetMapping("/login")
    @ResponseBody
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "regError", required = false) String regError,
                        @RequestParam(value = "regSuccess", required = false) String regSuccess) {

        String errorMsg = error != null ? "<div class='alert' style='color:red;'>ユーザー名またはパスワードが違います</div>" : "";
        String regErrorMsg = regError != null ? "<div class='alert' style='color:red;'>このユーザー名は既に登録されています</div>" : "";
        String regSuccessMsg = regSuccess != null ? "<div class='alert' style='color:green;'>ユーザー登録が完了しました！ログインしてください</div>" : "";

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>ログイン</title>
            <link rel="stylesheet" href="/css/style.css">
        </head>
        <body>
            <div class="container">
                <!-- ログインフォーム -->
                <div id="loginBox">
                    <h1>ログイン</h1>
                    """ + errorMsg + regErrorMsg + regSuccessMsg + """
                    <form action="/login" method="post">
                        <label>ユーザー名:</label>
                        <input type="text" name="username" required><br><br>
                        <label>パスワード:</label>
                        <input type="password" name="password" required><br><br>
                        <button type="submit">ログイン</button>
                    </form>
                    <p style="margin-top:20px; font-size:14px;">アカウントをお持ちでないですか？ 
                        <a href="javascript:void(0);" onclick="toggleForm()" style="color:#007bff; font-weight:bold;">新規登録はこちら</a>
                    </p>
                </div>

                <!-- 新規登録フォーム -->
                <div id="registerBox" style="display:none;">
                    <h1>新規ユーザー登録</h1>
                    <form action="/register" method="post">
                        <label>新しいユーザー名:</label>
                        <input type="text" name="username" required><br><br>
                        <label>新しいパスワード:</label>
                        <input type="password" name="password" required><br><br>
                        <button type="submit" style="background-color:#28a745;">登録する</button>
                    </form>
                    <p style="margin-top:20px; font-size:14px;">
                        <a href="javascript:void(0);" onclick="toggleForm()" style="color:#6c757d; font-weight:bold;">◀ ログイン画面に戻る</a>
                    </p>
                </div>
            </div>
            <script src="/js/script.js"></script>
        </body>
        </html>
        """;
    }

    // 2. 新規ユーザー登録処理（重複チェック ＆ 動的ログイン連携を完璧に実装）
    @PostMapping("/register")
    public String registerUser(@RequestParam("username") String username,
                               @RequestParam("password") String password) {

        // すでにユーザー名が存在するか重複チェック
        if (userManager.userExists(username)) {
            return "redirect:/login?regError=true"; // 重複注意メッセージへ
        }

        // 新しいユーザーをSpring Securityの防犯システムに直接登録
        UserDetails newUser = User.withUsername(username)
                .password("{noop}" + password)
                .roles("USER")
                .build();
        userManager.createUser(newUser);

        return "redirect:/login?regSuccess=true"; // 登録成功メッセージへ
    }

    // 3. カレンダー画面
    @GetMapping("/calendar")
    @ResponseBody
    public String calendar(HttpServletRequest request) {
        Authentication authentication = (Authentication) request.getUserPrincipal();
        String loggedInUser = authentication != null ? authentication.getName() : "defaultUser";

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>カレンダー</title>
            <link rel="stylesheet" href="/css/style.css">
            <script>
                const loggedInUser = "%s";
            </script>
        </head>
        <body>
            <div class="container">
                <div id="logoutContainer">
                    <form action="/logout" method="post">
                        <button type="submit" style="background-color: #dc3545; padding: 6px 12px; font-size:12px;">ログアウト</button>
                    </form>
                </div>
                <h1>スケジュールカレンダー</h1>
                <div id="controls">
                    <button id="prevMonth">前月</button>
                    <span id="monthYear"></span>
                    <button id="nextMonth">次月</button>
                </div>
                <table id="calendarTable">
                    <thead>
                        <tr>
                            <th class="sun">日</th><th>月</th><th>火</th><th>水</th><th>木</th><th>金</th><th class="sat">土</th>
                        </tr>
                    </thead>
                    <tbody id="calendarBody"></tbody>
                </table>
            </div>

            <!-- スケジュール追加ポップアップ（モーダル） -->
            <div id="scheduleModal" class="modal">
                <div class="modal-content">
                    <span class="close-button" id="closeModal">&times;</span>
                    <h2>スケジュール追加</h2>
                    <div>
                        <input type="hidden" id="year">
                        <input type="hidden" id="month">
                        <input type="hidden" id="day">
                        <label for="title">タイトル:</label>
                        <input type="text" id="title" required>
                        <label for="time">時間:</label>
                        <input type="time" id="time" required>
                        <label for="memo">メモ:</label>
                        <textarea id="memo" rows="3"></textarea>
                        <br><br>
                        <button id="saveSchedule">保存</button>
                    </div>
                </div>
            </div>

            <script src="/js/script.js"></script>
        </body>
        </html>
        """.formatted(loggedInUser);
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }
}

// 4. 防犯システムの設計図（競合を排除したクリーン設計）
@Configuration
@EnableWebSecurity
class CombinedSecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/calendar", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );
        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        return CalendarController.userManager; // 上で定義した共有マネージャーを直接繋ぎます
    }
}