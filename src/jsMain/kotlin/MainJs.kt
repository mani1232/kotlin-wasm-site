import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSpanElement

fun main() {

    val updateTimeButton = document.getElementById("time-button") as HTMLButtonElement
    updateTimeButton.addEventListener("click", {
        val updateTime = document.getElementById("time-output") as HTMLSpanElement
        val timeInput = document.getElementById("time-input") as HTMLInputElement
        updateTime(timeInput, updateTime)
    })
    val themeButton = document.getElementById("theme-button") as HTMLButtonElement
    themeButton.addEventListener("click", {
        val theme = if (document.documentElement?.getAttribute("data-bs-theme") == "dark") {
            "light"
        } else {
            "dark"
        }
        window.alert("After click theme changed to $theme")
        document.documentElement?.setAttribute("data-bs-theme", theme)
    })
}

val progress = "⡆⠇⠋⠙⠸⢰⣠⣄".map(Char::toString)

private fun updateTime(input: HTMLInputElement, output: HTMLSpanElement) {
    input.value = input.value.trim()
    var i = 0
    val progressId = window.setInterval({
        output.textContent = progress[i]
        i = (i + 1) % progress.size
        null
    }, 100)

    window.fetch("https://worldtimeapi.org/api/timezone/${input.value}").then {
            window.clearInterval(progressId)

            if (it.ok) {
                it.json().then {
                    output.textContent =
                        (it as WorldTimeApiResponse).datetime?.substringAfter("T")?.substringBefore(".") ?: "🧐"
                    null
                }
            } else {
                output.textContent = "🤷 " + it.status
            }
            null
        }.catch {
            window.clearInterval(progressId)
            output.textContent = "🙅🛜"
            null
        }
}

external interface WorldTimeApiResponse {
    val datetime: String?
}