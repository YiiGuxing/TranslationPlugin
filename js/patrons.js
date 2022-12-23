(function () {
    var wrapper = document.querySelector(".wechat-alipay-donors");
    if (!wrapper) {
        return;
    }

    function processPatronList(patronsTableBody, patronList) {
        var regx = /^\|\s*(.+?)\s*\|\s*(\[\s*(.+?)\s*]\(\s*(.+?)\s*\))?\s*\|\s*(.+?)\s*\|$/;
        for (var i = 0; i < patronList.length; i++) {
            var patronLine = patronList[i];
            if (patronLine) {
                var parsedPatron = regx.exec(patronLine);
                var name = parsedPatron[1];
                var websiteName = parsedPatron[3];
                var websiteUrl = parsedPatron[4];
                var website = null;
                if (websiteName && websiteUrl) {
                    website = {name: websiteName, url: websiteUrl};
                }
                var amount = parsedPatron[5];
                if (name && amount) {
                    appendPatron(patronsTableBody, name, website, amount);
                }
            }
        }
    }


    function appendPatron(patronsTableBody, name, website, amount) {
        var nameTd = "<td>" + name + "</td>";
        var websiteTd = "<td>" + (website ? "<a href='" + website.url + "' target='_blank'>" + website.name + "</a>" : "") + "</td>";
        var amountTd = "<td>" + amount + "</td>";
        var tr = document.createElement("tr");
        tr.innerHTML = nameTd + websiteTd + amountTd;

        patronsTableBody.append(tr)
    }


    function init(el) {
        var patronsTableBody = createPatronTable(el);
        var placeholder = createPlaceholder(el);

        loadData(patronsTableBody, placeholder);
    }

    function createPatronTable(el) {
        var table = document.createElement("table");
        var thead = document.createElement("thead");
        thead.innerHTML = "<tr>" +
            "<th class='donor'>" +
            "<span lang='zh'>名字</span>" +
            "<span lang='en'>Name</span>" +
            "<span lang='ja'>お名前</span>" +
            "<span lang='ko'>이름</span>" +
            "</th>" +
            "<th class='website'>" +
            "<span lang='zh'>网站</span>" +
            "<span lang='en'>Website</span>" +
            "<span lang='ja'>ウェブサイト</span>" +
            "<span lang='ko'>웹 사이트</span>" +
            "</th>" +
            "<th class='amount'>" +
            "<span lang='zh'>捐赠金额</span>" +
            "<span lang='en'>Donation Amount</span>" +
            "<span lang='ja'>寄付金額</span>" +
            "<span lang='ko'>기부액</span>" +
            "</th>" +
            "</tr>";

        var tbody = document.createElement("tbody");
        table.append(thead, tbody);
        el.append(table);
        return tbody;
    }

    function createPlaceholder(el) {
        var placeholder = document.createElement("div");
        placeholder.className = "placeholder";
        placeholder.innerHTML = "<span lang='zh'>加载中...</span>" +
            "<span lang='en'>Loading...</span>" +
            "<span lang='ja'>読み込み中...</span>" +
            "<span lang='ko'>로드 중...</span>";
        el.append(placeholder);
        return placeholder;
    }

    function loadData(patronsTableBody, placeholder) {
        var date = new Date()
        var t = "" + date.getFullYear() + (date.getMonth() + 1) + date.getDate()
        var xhr = new XMLHttpRequest();
        xhr.onreadystatechange = function () {
            if (xhr.readyState === 4) {
                if (xhr.status === 200) {
                    var tableHeader = "| -------- | -------- | ------------ |\n";
                    var index = xhr.responseText.indexOf(tableHeader);
                    var tableBody = xhr.responseText.substring(index + tableHeader.length);

                    processPatronList(patronsTableBody, tableBody.split(/\s*\n+\s*/));
                    placeholder.style.display = "none";
                } else {
                    placeholder.innerText = "加载失败！";
                }
            }
        };
        xhr.open("GET", "https://cdn.jsdelivr.net/gh/YiiGuxing/TranslationPlugin@master/FINANCIAL_CONTRIBUTORS.md?t=" + t, true);
        xhr.send();
    }

    init(wrapper);
})();
