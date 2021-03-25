window.onload = function () {
    var searchParams = new URLSearchParams(window.location.search);
    if (searchParams.get('editor') === "true") {
        var classList = document.querySelector("body").classList;
        classList.add("editor");
        if (searchParams.get('dark') === "true") {
            classList.add("dark");
        }
    }
};
