package com.lkacz.pola

import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

object MarkdownRenderer {
    private val extensions = listOf(TablesExtension.create())
    private val parser = Parser.builder().extensions(extensions).build()
    private val renderer =
        HtmlRenderer
            .builder()
            .extensions(extensions)
            .escapeHtml(false)
            .build()

    private const val STYLED_HTML_TEMPLATE =
        """
        <!DOCTYPE html>
        <html lang=\"en\">
        <head>
            <meta charset=\"utf-8\" />
            <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
            <style>
                :root { color-scheme: light dark; }
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    line-height: 1.6;
                    margin: 0;
                    padding: 18px 22px 36px;
                    color: inherit;
                    background: transparent;
                }
                h1, h2, h3, h4 { line-height: 1.25; }
                h1 { margin-top: 0; font-size: clamp(2rem, 3vw, 2.6rem); }
                h2 { margin-top: 32px; font-size: clamp(1.4rem, 2.5vw, 2rem); }
                h3 { margin-top: 28px; font-size: clamp(1.2rem, 2vw, 1.6rem); }
                p { margin: 0 0 14px; }
                ul, ol { margin: 0 0 16px 22px; padding: 0; }
                table { border-collapse: collapse; width: 100%; margin: 16px 0; }
                th, td { border: 1px solid rgba(128,128,128,0.35); padding: 8px 12px; text-align: left; vertical-align: top; }
                code { font-family: "JetBrains Mono", "Fira Code", "Cascadia Code", monospace; font-size: 0.95em; background: rgba(128,128,128,0.12); padding: 0.15em 0.35em; border-radius: 4px; }
                pre { background: rgba(128,128,128,0.12); padding: 12px; border-radius: 8px; overflow-x: auto; }
                blockquote { border-left: 4px solid rgba(63,81,181,0.45); margin: 16px 0; padding: 0 16px; color: inherit; }
                a { color: #1a73e8; text-decoration: none; }
                a:hover, a:focus { text-decoration: underline; }
                hr { border: none; border-top: 1px solid rgba(128,128,128,0.25); margin: 32px 0; }
                .tip { border-left: 4px solid rgba(63,81,181,0.7); background: rgba(63,81,181,0.08); padding: 12px 16px; border-radius: 8px; margin: 18px 0; }
            </style>
        </head>
        <body class=\"markdown-body\">
        {{CONTENT}}
        </body>
        </html>
        """

    fun render(markdown: String): String {
        if (markdown.startsWith("Error")) return markdown
        val document: Node = parser.parse(markdown)
        val html = renderer.render(document)
        return STYLED_HTML_TEMPLATE.replace("{{CONTENT}}", html)
    }
}
