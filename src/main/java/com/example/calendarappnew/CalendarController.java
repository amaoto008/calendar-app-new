package com.example.calendarappnew;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class CalendarController {

    // ログイン画面：Javaから直接HTMLを送り出す（CSRF用の隠しデータがないため、下で無効化します）
    @GetMapping("/login")
    @ResponseBody
    public String login() {
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
                <h1>ログイン</h1>
                <form action="/login" method="post">
                    <label for="username">ユーザー名:</label>
                    <input type="text" id="username" name="username" required>
                    <br><br>
                    <label for="password">パスワード:</label>
                    <input type="password" id="password" name="password" required>
                    <br><br>
                    <button type="submit">ログイン</button>
                </form>
            </div>
        </body>
        </html>
        """;
    }

    // カレンダー画面
    @GetMapping("/calendar")
    @ResponseBody
    public String calendar() {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>カレンダー</title>
            <link rel="stylesheet" href="/css/style.css">
            <script src="/js/script.js" defer></script>
        </head>
        <body>
            <div class="container">
                <div id="controls">
                    <button id="prevMonth">前の月</button>
                    <span id="monthYear"></span>
                    <button id="nextMonth">次の月</button>
                </div>
                <table id="calendarTable">
                    <thead>
                        <tr>
                            <th>日</th>
                            <th>月</th>
                            <th>火</th>
                            <th>水</th>
                            <th>木</th>
                            <th>金</th>
                            <th>土</th>
                        </tr>
                    </thead>
                    <tbody id="calendarBody"></tbody>
                </table>
            </div>
            <div id="scheduleModal" class="modal">
                <div class="modal-content">
                    <span class="close-button">&times;</span>
                    <h2>スケジュール追加</h2>
                    <label for="title">タイトル:</label>
                    <input type="text" id="title" required>
                    <br><br>
                    <label for="time">時間:</label>
                    <input type="time" id="time" required>
                    <br><br>
                    <label for="memo">メモ:</label>
                    <textarea id="memo" required></textarea>
                    <br><br>
                    <input type="hidden" id="year" required>
                    <input type="hidden" id="month" required>
                    <input type="hidden" id="day" required>
                    <button id="saveSchedule">保存</button>
                </div>
            </div>
            <div id="logoutContainer">
                <button id="logoutButton">ログアウト</button>
            </div>
        </body>
        </html>
        """;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }
}

@Configuration
@EnableWebSecurity
class CombinedSecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 【ここを追加！】自作HTMLからのログインを通すために、一時的にCSRF対策をオフにします
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/error").permitAll()
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
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withUsername("user")
                .password("{noop}pass")
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}