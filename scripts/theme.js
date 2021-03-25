window.onload = function () {
    var searchParams = new URLSearchParams(window.location.search);
    if (searchParams.get('editor')) {
        var classList = document.querySelector("body").classList;
        classList.add("editor");
        if (searchParams.get('dark')) {
            classList.add("dark");
        }
    }
};
