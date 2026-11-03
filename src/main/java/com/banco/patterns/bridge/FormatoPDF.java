package com.banco.patterns.bridge;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** PATRON BRIDGE (Implementador concreto): exporta a PDF con los colores de Banco NEXO (libreria OpenPDF). */
public class FormatoPDF implements FormatoReporte {

    private static final Color VERDE_NEXO = new Color(0x0B, 0x7A, 0x4B);
    private static final Color GRIS_FILA = new Color(0xF2, 0xF5, 0xF3);

    @Override
    public byte[] exportar(String titulo, List<String> columnas, List<List<String>> filas) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document doc = new Document(columnas.size() > 5 ? PageSize.A4.rotate() : PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, salida);
            doc.open();
            Font marca = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, VERDE_NEXO);
            Font tituloFuente = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Font nota = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            doc.add(new Paragraph("Banco NEXO", marca));
            doc.add(new Paragraph(titulo, tituloFuente));
            doc.add(new Paragraph("Generado el " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + " - " + filas.size() + " registros", nota));
            doc.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(columnas.size());
            tabla.setWidthPercentage(100);
            tabla.setHeaderRows(1);
            Font cabecera = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font celda = FontFactory.getFont(FontFactory.HELVETICA, 8);
            for (String c : columnas) {
                PdfPCell cell = new PdfPCell(new Phrase(c, cabecera));
                cell.setBackgroundColor(VERDE_NEXO);
                cell.setPadding(5);
                tabla.addCell(cell);
            }
            int n = 0;
            for (List<String> fila : filas) {
                for (String valor : fila) {
                    PdfPCell cell = new PdfPCell(new Phrase(valor == null ? "" : valor, celda));
                    cell.setPadding(4);
                    if (n % 2 == 1) {
                        cell.setBackgroundColor(GRIS_FILA);
                    }
                    if (valor != null && valor.startsWith("S/ ")) {
                        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    }
                    tabla.addCell(cell);
                }
                n++;
            }
            doc.add(tabla);
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        } finally {
            doc.close();
        }
        return salida.toByteArray();
    }

    @Override
    public String extension() { return "pdf"; }

    @Override
    public String tipoContenido() { return "application/pdf"; }
}
