// 削除イベントをグローバルスコープに公開
window.deleteEvent = function(dateKey, index) {
    const userEventsKey = 'events_' + loggedInUser;
    const events = JSON.parse(localStorage.getItem(userEventsKey)) || [];
    const dayEvents = events.filter(event => `${event.year}-${event.month}-${event.day}` === dateKey);
    if (index >= 0 && index < dayEvents.length) {
        const eventToRemove = dayEvents[index];
        const eventIndexInEvents = events.findIndex(event => event.id === eventToRemove.id);
        if (eventIndexInEvents !== -1) {
            events.splice(eventIndexInEvents, 1);
            localStorage.setItem(userEventsKey, JSON.stringify(events));
            window.refreshCalendar();
        }
    }
};

// ログインフォームと新規登録フォームの切り替えをグローバルスコープに公開
window.toggleForm = function() {
    var loginBox = document.getElementById('loginBox');
    var registerBox = document.getElementById('registerBox');
    if (loginBox.style.display === 'block' || loginBox.style.display === '') {
        loginBox.style.display = 'none';
        registerBox.style.display = 'block';
    } else {
        loginBox.style.display = 'block';
        registerBox.style.display = 'none';
    }
};

document.addEventListener('DOMContentLoaded', function() {
    const prevMonthButton = document.getElementById('prevMonth');
    const nextMonthButton = document.getElementById('nextMonth');
    const monthYear = document.getElementById('monthYear');
    const calendarBody = document.getElementById('calendarBody');
    const scheduleModal = document.getElementById('scheduleModal');
    const closeModal = document.querySelector('.close-button');
    const saveScheduleButton = document.getElementById('saveSchedule');

    const today = new Date();
    let currentMonth = today.getMonth();
    let currentYear = today.getFullYear();

    // renderCalendar 関数を定義
    function renderCalendar() {
        const firstDay = new Date(currentYear, currentMonth, 1).getDay();
        const lastDate = new Date(currentYear, currentMonth + 1, 0).getDate();
        monthYear.textContent = `${currentYear}年 ${currentMonth + 1}月`;

        let html = '';
        let day = 1;

        for (let i = 0; i < 6; i++) {
            let row = '<tr>';
            for (let j = 0; j < 7; j++) {
                if (i === 0 && j < firstDay) {
                    row += '<td></td>';
                } else if (day > lastDate) {
                    row += '<td></td>';
                } else {
                    const dayEvents = getEventsForDate(currentYear, currentMonth + 1, day);
                    let dayEventsHtml = '';
                    dayEvents.forEach((event, idx) => {
                        // 【★最大の修正ポイント】
                        // 予定のタイトルを <span class="event-text"> で囲みました。
                        // これにより、CSSの切り詰め（...）の魔法が文字だけに100%効くようになり、×ボタンを守ります。
                        dayEventsHtml += `
                        <div class="event">
                            <span class="event-text">${event.title}</span> 
                            <button class="delete-button" onclick="event.stopPropagation(); deleteEvent('${currentYear}-${currentMonth + 1}-${day}', ${idx})">×</button>
                        </div>`;
                    });
                    row += `<td onclick="openScheduleModal(${currentYear}, ${currentMonth + 1}, ${day})">
                                <div class="date-num">${day}</div>
                                ${dayEventsHtml}
                            </td>`;
                    day++;
                }
            }
            row += '</tr>';
            html += row;
            if (day > lastDate) break;
        }
        calendarBody.innerHTML = html;
    }

    // renderCalendar をグローバルスコープに公開
    window.refreshCalendar = renderCalendar;

    renderCalendar();

    if (prevMonthButton) {
        prevMonthButton.addEventListener('click', function() {
            currentMonth = currentMonth === 0 ? 11 : currentMonth - 1;
            if (currentMonth === 11) currentYear--;
            renderCalendar();
        });
    }

    if (nextMonthButton) {
        nextMonthButton.addEventListener('click', function() {
            currentMonth = currentMonth === 11 ? 0 : currentMonth + 1;
            if (currentMonth === 0) currentYear++;
            renderCalendar();
        });
    }

    if (closeModal) {
        closeModal.onclick = function() {
            scheduleModal.style.display = 'none';
        }
    }

    window.onclick = function(event) {
        if (event.target == scheduleModal) {
            scheduleModal.style.display = 'none';
        }
    }

    saveScheduleButton.addEventListener('click', function() {
        const title = document.getElementById('title').value;
        const time = document.getElementById('time').value;
        const memo = document.getElementById('memo').value;
        const year = document.getElementById('year').value;
        const month = document.getElementById('month').value;
        const day = document.getElementById('day').value;

        const event = { title, time, memo, year, month, day, id: Date.now().toString() };
        saveEvent(event);
        renderCalendar();
        scheduleModal.style.display = 'none';
    });

    function getEventsForDate(year, month, day) {
        const userEventsKey = 'events_' + loggedInUser;
        const events = JSON.parse(localStorage.getItem(userEventsKey)) || [];
        return events.filter(event => event.year == year && event.month == month && event.day == day);
    }

    function saveEvent(event) {
        const userEventsKey = 'events_' + loggedInUser;
        const events = JSON.parse(localStorage.getItem(userEventsKey)) || [];
        events.push(event);
        localStorage.setItem(userEventsKey, JSON.stringify(events));
    }
});

function openScheduleModal(year, month, day) {
    document.getElementById('year').value = year;
    document.getElementById('month').value = month;
    document.getElementById('day').value = day;
    document.getElementById('scheduleModal').style.display = 'block';
}