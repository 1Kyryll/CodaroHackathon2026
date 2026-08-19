(function () {
    var form = document.getElementById("queue-filter");
    if (!form) {
        return;
    }
    var dateInput = form.querySelector("input[name='date']");
    if (!dateInput) {
        return;
    }
    dateInput.addEventListener("change", function () {
        if (dateInput.value) {
            form.requestSubmit();
        }
    });
})();
