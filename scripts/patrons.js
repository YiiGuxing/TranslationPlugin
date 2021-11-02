(function () {
    function processPatronsList(patronsList) {
        var regx = /^\|\s*(.+?)\s*\|\s*(\[\s*(.+?)\s*]\(\s*(.+?)\s*\))?\s*\|\s*(.+?)\s*\|$/;
        for (var i = 0; i < patronsList.length; i++) {
            var patronsLine = patronsList[i];
            if (patronsLine) {
                var parsedPatron = regx.exec(patronsLine);
                var name = parsedPatron[1];
                var websiteName = parsedPatron[3];
                var websiteUrl = parsedPatron[4];
                var website = null;
                if (websiteName && websiteUrl) {
                    website = {name: websiteName, url: websiteUrl};
                }
                var amount = parsedPatron[5];
                if (name && amount) {
                    appendPatron(name, website, amount);
                }
            }
        }
    }

    var $patronsTableBody = $("#patronsTableBody");

    function appendPatron(name, website, amount) {
        var nameTd = "<td>" + name + "</td>";
        var websiteTd = "<td>" + (website ? "<a href='" + website.url + "' target='_blank'>" + website.name + "</a>" : "") + "</td>";
        var amountTd = "<td>" + amount + "</td>";
        $patronsTableBody.append($("<tr>" + nameTd + websiteTd + amountTd + "</tr>"))
    }

    var date = new Date()
    var t = "" + date.getFullYear() + (date.getMonth() + 1) + date.getDate()
    $.ajax({
        url: "https://cdn.jsdelivr.net/gh/YiiGuxing/TranslationPlugin@master/FINANCIAL_CONTRIBUTORS.md?t=" + t,
        type: "GET",
        dataType: "TEXT",
        success: function (data) {
            var tableHeader = "| -------- | -------- | ------------ |\n";
            var index = data.indexOf(tableHeader);
            var tableBody = data.substring(index + tableHeader.length);

            processPatronsList(tableBody.split(/\s*\n+\s*/));
            $("#placeholder").hide();
        },
        error: function () {
            $("#placeholder").text("加载失败！")
        }
    });
})();
