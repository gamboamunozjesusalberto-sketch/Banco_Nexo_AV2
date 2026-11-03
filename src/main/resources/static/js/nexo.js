/* Banco NEXO - pequenas interacciones de la interfaz (sin librerias aparte de Bootstrap). */
(function () {
    "use strict";

    // 1. Mensajes (ok / error) como toasts que se cierran solos
    document.querySelectorAll(".toast").forEach(function (el) {
        new bootstrap.Toast(el, { delay: el.classList.contains("error") ? 8000 : 5000 }).show();
    });

    // 2. Ocultar / mostrar saldos (como en las apps de BCP o Interbank)
    var CLAVE_SALDO = "nexo-saldo-oculto";
    function aplicarSaldo(oculto) {
        document.body.classList.toggle("saldo-oculto", oculto);
        document.querySelectorAll("[data-toggle-saldo]").forEach(function (b) {
            b.innerHTML = oculto ? "<svg class=\"ico\"><use href=\"/img/iconos.svg#ojo\"></use></svg> Mostrar saldo" : "<svg class=\"ico\"><use href=\"/img/iconos.svg#ojo-no\"></use></svg> Ocultar saldo";
        });
    }
    var oculto = false;
    try { oculto = localStorage.getItem(CLAVE_SALDO) === "1"; } catch (e) { /* sin almacenamiento */ }
    aplicarSaldo(oculto);
    document.querySelectorAll("[data-toggle-saldo]").forEach(function (b) {
        b.addEventListener("click", function () {
            oculto = !oculto;
            try { localStorage.setItem(CLAVE_SALDO, oculto ? "1" : "0"); } catch (e) { /* sin almacenamiento */ }
            aplicarSaldo(oculto);
        });
    });

    // 3. Copiar numero de cuenta
    document.querySelectorAll("[data-copiar]").forEach(function (b) {
        b.addEventListener("click", function () {
            var texto = b.getAttribute("data-copiar");
            if (navigator.clipboard) { navigator.clipboard.writeText(texto); }
            var antes = b.innerHTML;
            b.innerHTML = "<svg class=\"ico\"><use href=\"/img/iconos.svg#check\"></use></svg> Copiado";
            setTimeout(function () { b.innerHTML = antes; }, 1500);
        });
    });

    // 4. Montos altos: si el monto llega al umbral, aparece el campo para confirmar con la clave
    document.querySelectorAll("form[data-umbral]").forEach(function (form) {
        var umbral = parseFloat(form.getAttribute("data-umbral"));
        var monto = form.querySelector("input[name=monto]");
        var bloque = form.querySelector(".bloque-confirmacion");
        var clave = bloque ? bloque.querySelector("input") : null;
        function revisar() {
            var alto = parseFloat(monto.value || "0") >= umbral;
            if (bloque) { bloque.classList.toggle("d-none", !alto); clave.required = alto; }
        }
        if (monto) { monto.addEventListener("input", revisar); revisar(); }
    });

    // 5. Ventana de confirmacion antes de mover dinero
    document.querySelectorAll("form[data-confirmar]").forEach(function (form) {
        form.addEventListener("submit", function (ev) {
            if (form.dataset.confirmado === "1") { return; }
            ev.preventDefault();
            var modalEl = document.getElementById("modalConfirmar");
            if (!modalEl) { form.dataset.confirmado = "1"; form.submit(); return; }
            var resumen = "";
            form.querySelectorAll("[data-resumen]").forEach(function (campo) {
                var valor = campo.tagName === "SELECT" ? campo.options[campo.selectedIndex].text : campo.value;
                if (campo.name === "monto") { valor = "S/ " + Number(valor || 0).toLocaleString("es-PE", { minimumFractionDigits: 2 }); }
                if (valor) { resumen += "<div class='d-flex justify-content-between border-bottom py-2'><span class='text-muted'>"
                    + campo.getAttribute("data-resumen") + "</span><strong class='text-end ms-3'></strong></div>"; }
            });
            var cuerpo = modalEl.querySelector(".resumen");
            cuerpo.innerHTML = resumen;
            // se escribe con textContent para no interpretar HTML del usuario
            var i = 0;
            form.querySelectorAll("[data-resumen]").forEach(function (campo) {
                var valor = campo.tagName === "SELECT" ? campo.options[campo.selectedIndex].text : campo.value;
                if (campo.name === "monto") { valor = "S/ " + Number(valor || 0).toLocaleString("es-PE", { minimumFractionDigits: 2 }); }
                if (valor) { cuerpo.querySelectorAll("strong")[i++].textContent = valor; }
            });
            var modal = bootstrap.Modal.getOrCreateInstance(modalEl);
            modalEl.querySelector(".btn-confirmar").onclick = function () {
                form.dataset.confirmado = "1";
                modal.hide();
                form.submit();
            };
            modal.show();
        });
    });

    // 6. Simulador de credito (cuota fija, metodo frances) - mismo calculo que SolicitudCredito.getCuotaMensual()
    var sim = document.getElementById("simulador");
    if (sim) {
        var tasaAnual = parseFloat(sim.getAttribute("data-tasa"));
        var inMonto = document.getElementById("simMonto"), inPlazo = document.getElementById("simPlazo");
        var salida = document.getElementById("simCuota"), total = document.getElementById("simTotal");
        function calcular() {
            var p = parseFloat(inMonto.value || "0"), n = parseInt(inPlazo.value || "0", 10);
            if (!(p > 0) || !(n > 0)) { salida.textContent = "S/ 0.00"; total.textContent = "S/ 0.00"; return; }
            var i = tasaAnual / 100 / 12;
            var cuota = i === 0 ? p / n : p * i / (1 - Math.pow(1 + i, -n));
            var fmt = function (v) { return "S/ " + v.toLocaleString("es-PE", { minimumFractionDigits: 2, maximumFractionDigits: 2 }); };
            salida.textContent = fmt(cuota);
            total.textContent = fmt(cuota * n);
        }
        inMonto.addEventListener("input", calcular);
        inPlazo.addEventListener("input", calcular);
        calcular();
    }

    // 7. Medidor de fuerza de la clave
    document.querySelectorAll("input[data-medidor]").forEach(function (input) {
        var barra = document.querySelector(input.getAttribute("data-medidor") + " > span");
        var texto = document.querySelector(input.getAttribute("data-medidor") + "-texto");
        input.addEventListener("input", function () {
            var v = input.value, puntos = 0;
            if (v.length >= 8) puntos++;
            if (/[a-zA-Z]/.test(v) && /[0-9]/.test(v)) puntos++;
            if (/[A-Z]/.test(v) && /[a-z]/.test(v)) puntos++;
            if (/[^a-zA-Z0-9]/.test(v) || v.length >= 12) puntos++;
            var colores = ["#C8372D", "#E8A317", "#9BE15D", "#0B7A4B", "#0B7A4B"];
            var nombres = ["Muy debil", "Debil", "Aceptable", "Segura", "Muy segura"];
            barra.style.width = (v.length === 0 ? 0 : 25 * Math.max(puntos, 1)) + "%";
            barra.style.backgroundColor = colores[puntos];
            if (texto) { texto.textContent = v.length === 0 ? "" : nombres[puntos]; }
        });
    });

    // 8. Barras de reportes: se animan al cargar
    requestAnimationFrame(function () {
        document.querySelectorAll("[data-ancho]").forEach(function (b) { b.style.width = b.getAttribute("data-ancho") + "%"; });
        document.querySelectorAll("[data-alto]").forEach(function (b) { b.style.height = b.getAttribute("data-alto") + "%"; });
    });

    // 9. Filtro rapido de tablas
    document.querySelectorAll("input[data-filtrar]").forEach(function (input) {
        var tabla = document.querySelector(input.getAttribute("data-filtrar"));
        input.addEventListener("input", function () {
            var q = input.value.toLowerCase();
            tabla.querySelectorAll("tbody tr").forEach(function (tr) {
                tr.style.display = tr.textContent.toLowerCase().indexOf(q) >= 0 ? "" : "none";
            });
        });
    });

    // 10. Mostrar / ocultar clave en los campos de contrasena
    document.querySelectorAll("[data-ver-clave]").forEach(function (b) {
        b.addEventListener("click", function () {
            var input = document.querySelector(b.getAttribute("data-ver-clave"));
            input.type = input.type === "password" ? "text" : "password";
            b.innerHTML = input.type === "password" ? "<svg class=\"ico\"><use href=\"/img/iconos.svg#ojo\"></use></svg>" : "<svg class=\"ico\"><use href=\"/img/iconos.svg#ojo-no\"></use></svg>";
        });
    });
})();
