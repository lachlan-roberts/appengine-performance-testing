/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.logging.Logger;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.statistic.CounterStatistic;

public class SizedResponseServlet extends HttpServlet {
    private static final CounterStatistic statistic = new CounterStatistic();
    private final ThreadLocal<byte[]> bytes = new ThreadLocal<>();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        statistic.increment();
        try
        {
            String reset = req.getParameter("reset");
            if (reset != null)
            {
                statistic.reset();
                resp.sendRedirect("/?stats");
                return;
            }

            String stats = req.getParameter("stats");
            if (stats != null)
            {
                resp.setContentType("text/html");
                String pathInContext = URIUtil.addPaths(req.getContextPath(), req.getServletPath());
                if (!pathInContext.endsWith("/"))
                    pathInContext = pathInContext + "/";

                PrintWriter writer = resp.getWriter();
                writer.println("current: " + statistic.getCurrent() + "<br>");
                writer.println("max: " + statistic.getMax() + "<br>");
                writer.println("total: " + statistic.getTotal() + "<br>");
                writer.println("<a href=\"" + pathInContext + "?reset\">Reset</a>");
                return;
            }

            resp.setContentType("text/plain");
            String bufferSizeParam = req.getParameter("bufferSize");
            String numWritesParam = req.getParameter("numWrites");
            long numWrites = (numWritesParam == null) ? 0 : Long.parseLong(numWritesParam);
            int bufferSize = (bufferSizeParam == null) ? 0 : Integer.parseInt(bufferSizeParam);

            byte[] b = bytes.get();
            if (b == null || b.length != bufferSize)
            {
                b = new byte[bufferSize];
                bytes.set(b);
            }

            Arrays.fill(b, (byte)'x');
            ServletOutputStream outputStream = resp.getOutputStream();
            for (int i = 0; i < numWrites; i++)
            {
                outputStream.write(b);
            }
        }
        finally
        {
            statistic.decrement();
        }
    }
}
