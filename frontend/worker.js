
const API_BASE_URL = "http://localhost:8080/api";

const $ = (id) => document.getElementById(id);

let childrenCache = [];
let recordsCache = new Map();

function getSession() {
  try {
    return JSON.parse(
      sessionStorage.getItem("poshancare_session") || "null"
    );
  } catch {
    return null;
  }
}

function requireWorkerSession() {
  const session = getSession();

  if (!session?.token || session.role !== "WORKER") {
    window.location.href = "login.html";
    return null;
  }

  return session;
}

async function apiRequest(path, options = {}) {
  const session = requireWorkerSession();
  if (!session) throw new Error("Worker login required.");

  const headers = {
    Accept: "application/json",
    Authorization: `Bearer ${session.token}`,
    ...(options.body ? { "Content-Type": "application/json" } : {}),
    ...options.headers
  };

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers
  });

  if (response.status === 401) {
    sessionStorage.removeItem("poshancare_session");
    window.location.href = "login.html";
    throw new Error("Your session expired. Please log in again.");
  }

  if (!response.ok) {
    const message = await response.text();
    throw new Error(
      message || `Request failed (${response.status}).`
    );
  }

  if (response.status === 204) return null;

  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

function today() {
  const now = new Date();
  const offset = now.getTimezoneOffset();

  return new Date(now.getTime() - offset * 60000)
    .toISOString()
    .slice(0, 10);
}

function escapeHTML(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  })[char]);
}

function latestMeasurement(childId) {
  const records = recordsCache.get(childId)?.growth || [];

  return [...records].sort((a, b) =>
    String(b.measurementDate).localeCompare(
      String(a.measurementDate)
    )
  )[0] || null;
}

function riskStatus() {
  // No validated medical assessment is implemented.
  return "Not assessed";
}

function statusClass(status) {
  if (status === "Normal") return "status-normal";
  if (status === "Monitor") return "status-monitor";
  if (status === "High Risk") return "status-high";
  return "status-none";
}


async function loadChildRecords(childId) {
  const encodedId = encodeURIComponent(childId);

  const [growthResult, followupResult, nutritionResult] =
    await Promise.allSettled([
      apiRequest(`/children/${encodedId}/growth`),
      apiRequest(`/children/${encodedId}/followups`),
      apiRequest(`/children/${encodedId}/nutrition`)
    ]);

  recordsCache.set(childId, {
    growth: growthResult.status === "fulfilled"
      ? growthResult.value || []
      : [],
    followups: followupResult.status === "fulfilled"
      ? followupResult.value || []
      : [],
    nutrition: nutritionResult.status === "fulfilled"
      ? nutritionResult.value || []
      : [],
    growthError: growthResult.status === "rejected",
    followupError: followupResult.status === "rejected",
    nutritionError: nutritionResult.status === "rejected"
  });
}


async function renderDashboard() {
  $("emptyMessage").hidden = true;
  $("childTableBody").innerHTML =
    `<tr><td colspan="6">Loading child records...</td></tr>`;

  try {
    const result = await apiRequest("/children");

    if (!Array.isArray(result)) {
      throw new Error("Unexpected child list response.");
    }

    childrenCache = result;

    await Promise.all(
      childrenCache.map((child) => loadChildRecords(child.childId))
    );

    const counts = {
      Normal: 0,
      Monitor: 0,
      "High Risk": 0,
      "Not assessed": 0
    };

    let followupCount = 0;

    childrenCache.forEach((child) => {
      counts[riskStatus(child)]++;

      const followups =
        recordsCache.get(child.childId)?.followups || [];

      followupCount += followups.length;
    });

    $("totalChildren").textContent = childrenCache.length;
    $("normalCount").textContent = counts.Normal;
    $("monitorCount").textContent = counts.Monitor;

    // This card is labelled Follow-up in worker.html.
    $("highCount").textContent = followupCount;

    const query = $("searchInput").value.trim().toLowerCase();
    const filter = $("riskFilter").value;

    const filtered = childrenCache.filter((child) => {
      const name = String(child.name || "").toLowerCase();
      const id = String(child.childId || "").toLowerCase();

      return (
        (name.includes(query) || id.includes(query)) &&
        (filter === "All" || riskStatus(child) === filter)
      );
    });

    $("childTableBody").innerHTML = filtered.map((child) => {
      const measurement = latestMeasurement(child.childId);
      const status = riskStatus(child);

      return `
        <tr>
          <td>${escapeHTML(child.name)}</td>
          <td>${escapeHTML(child.childId)}</td>
          <td>${measurement
            ? `${escapeHTML(measurement.heightCm)} cm`
            : "—"}</td>
          <td>${measurement
            ? `${escapeHTML(measurement.weightKg)} kg`
            : "—"}</td>
          <td>
            <span class="status ${statusClass(status)}">
              ${escapeHTML(status)}
            </span>
          </td>
          <td>
            <button class="table-action"
              data-action="view"
              data-id="${escapeHTML(child.childId)}">
              View / Update
            </button>
          </td>
        </tr>`;
    }).join("");

    $("emptyMessage").hidden = filtered.length > 0;

    if (filtered.length === 0) {
      $("emptyMessage").textContent = query || filter !== "All"
        ? "No matching child records found."
        : "No child records found. Register a child to get started.";
    }
  } catch (error) {
    $("childTableBody").innerHTML = "";
    $("emptyMessage").hidden = false;
    $("emptyMessage").textContent =
      `Could not load child records: ${error.message}`;
  }
}


let growthChartInstance = null;

function renderGrowthChart(records) {
  const canvas = $("growthChart");
  const message = $("chartMessage");

  if (!canvas || !message) return;

  // Destroy the previous chart before drawing another child's chart.
  if (growthChartInstance) {
    growthChartInstance.destroy();
    growthChartInstance = null;
  }

  const sortedRecords = [...(records || [])]
    .filter((item) =>
      item.measurementDate &&
      Number.isFinite(Number(item.heightCm)) &&
      Number.isFinite(Number(item.weightKg))
    )
    .sort((a, b) =>
      String(a.measurementDate).localeCompare(
        String(b.measurementDate)
      )
    );

  if (sortedRecords.length === 0) {
    message.textContent =
      "No growth measurements yet. Save a measurement to start the chart.";
    message.hidden = false;
    return;
  }

  if (typeof Chart === "undefined") {
    message.textContent =
      "The chart library could not load. Check your internet connection.";
    message.hidden = false;
    return;
  }

  message.hidden = true;

  growthChartInstance = new Chart(canvas, {
    type: "line",

    data: {
      labels: sortedRecords.map(
        (item) => item.measurementDate
      ),

      datasets: [
        {
          label: "Height (cm)",
          data: sortedRecords.map(
            (item) => Number(item.heightCm)
          ),
          borderColor: "#16845b",
          backgroundColor: "rgba(22, 132, 91, 0.12)",
          tension: 0.25,
          yAxisID: "heightAxis"
        },
        {
          label: "Weight (kg)",
          data: sortedRecords.map(
            (item) => Number(item.weightKg)
          ),
          borderColor: "#3478c8",
          backgroundColor: "rgba(52, 120, 200, 0.12)",
          tension: 0.25,
          yAxisID: "weightAxis"
        }
      ]
    },

    options: {
      responsive: true,
      maintainAspectRatio: false,

      interaction: {
        mode: "index",
        intersect: false
      },

      plugins: {
        legend: {
          position: "bottom"
        },
        tooltip: {
          enabled: true
        }
      },

      scales: {
        x: {
          title: {
            display: true,
            text: "Measurement date"
          }
        },

        heightAxis: {
          type: "linear",
          position: "left",
          title: {
            display: true,
            text: "Height (cm)"
          }
        },

        weightAxis: {
          type: "linear",
          position: "right",
          title: {
            display: true,
            text: "Weight (kg)"
          },
          grid: {
            drawOnChartArea: false
          }
        }
      }
    }
  });
}



function renderNutritionHistory(records) {
  const container = $("nutritionHistory");
  if (!container) return;

  container.replaceChildren();

  if (!Array.isArray(records) || records.length === 0) {
    const message = document.createElement("p");
    message.className = "form-note";
    message.textContent = "No nutrition records recorded yet.";
    container.appendChild(message);
    return;
  }

  const fields = [
    ["foodConsumed", "Food consumed"],
    ["caloriesConsumedKcal", "Calories consumed (kcal)"],
    ["caloriesRequiredKcal", "Calories required (kcal)"],
    ["proteinConsumedG", "Protein consumed (g)"],
    ["proteinRequiredG", "Protein required (g)"],
    ["carbsConsumedG", "Carbohydrates consumed (g)"],
    ["carbsRequiredG", "Carbohydrates required (g)"],
    ["notes", "Notes"]
  ];

  const sorted = [...records].sort((a, b) =>
    String(b.recordDate || "").localeCompare(
      String(a.recordDate || "")
    )
  );

  sorted.forEach((record) => {
    const article = document.createElement("article");
    article.className = "nutrition-item";

    const heading = document.createElement("h4");
    heading.textContent = record.recordDate || "Nutrition record";
    article.appendChild(heading);

    fields.forEach(([key, label]) => {
      if (record[key] == null || record[key] === "") return;

      const line = document.createElement("p");
      line.textContent = `${label}: ${record[key]}`;
      article.appendChild(line);
    });

    container.appendChild(article);
  });
}


function renderNutritionSummary(records) {
  const container = $("nutritionSummary");
  if (!container) return;

  container.replaceChildren();

  if (!Array.isArray(records) || records.length === 0) {
    const message = document.createElement("p");
    message.className = "form-note";
    message.textContent = "No nutrition records are available yet.";
    container.appendChild(message);
    return;
  }

  const latest = [...records].sort((a, b) =>
    String(b.recordDate || "").localeCompare(
      String(a.recordDate || "")
    )
  )[0];

  const heading = document.createElement("p");
  heading.className = "nutrition-summary-date";
  heading.textContent =
    `Latest record: ${latest.recordDate || "Date not recorded"}`;
  container.appendChild(heading);

  const metrics = [
    {
      label: "Calories",
      consumed: latest.caloriesConsumedKcal,
      required: latest.caloriesRequiredKcal,
      unit: "kcal"
    },
    {
      label: "Protein",
      consumed: latest.proteinConsumedG,
      required: latest.proteinRequiredG,
      unit: "g"
    },
    {
      label: "Carbohydrates",
      consumed: latest.carbsConsumedG,
      required: latest.carbsRequiredG,
      unit: "g"
    }
  ];

  const grid = document.createElement("div");
  grid.className = "nutrition-summary-grid";

  metrics.forEach((metric) => {
    const card = document.createElement("article");
    card.className = "nutrition-metric-card";

    const title = document.createElement("h4");
    title.textContent = metric.label;

    const consumed = document.createElement("p");
    consumed.className = "nutrition-metric-value";
    consumed.textContent =
      metric.consumed == null
        ? "Not recorded"
        : `${metric.consumed} ${metric.unit}`;

    const required = document.createElement("p");
    required.className = "nutrition-metric-required";
    required.textContent =
      metric.required == null
        ? "Required: Not recorded"
        : `Required: ${metric.required} ${metric.unit}`;

    const comparison = document.createElement("p");
    comparison.className = "nutrition-metric-comparison";

    if (metric.consumed == null || metric.required == null) {
      comparison.textContent = "Comparison unavailable";
    } else {
      const difference = Number(metric.consumed) -
        Number(metric.required);

      comparison.textContent =
        `Difference: ${difference > 0 ? "+" : ""}${Number(difference.toFixed(2))} ${metric.unit}`;
    }

    card.append(title, consumed, required, comparison);
    grid.appendChild(card);
  });

  container.appendChild(grid);

  if (latest.foodConsumed) {
    const food = document.createElement("p");
    food.className = "nutrition-summary-food";
    food.textContent = `Food consumed: ${latest.foodConsumed}`;
    container.appendChild(food);
  }

  if (latest.notes) {
    const notes = document.createElement("p");
    notes.className = "nutrition-summary-notes";
    notes.textContent = `Notes: ${latest.notes}`;
    container.appendChild(notes);
  }
}




function openChildDetails(childId) {
  const child = childrenCache.find(
    (item) => item.childId === childId
  );

  if (!child) return;

  const records = recordsCache.get(childId);
  const nutritionForm = $("nutritionForm");

  nutritionForm.elements.childId.value = child.childId;
  nutritionForm.elements.recordDate.value = today();
  nutritionForm.elements.recordDate.max = today();

  $("nutritionMessage").textContent = "";

  renderNutritionHistory(records?.nutrition || []);
  renderNutritionSummary(records?.nutrition || []);

  if (records?.nutritionError) {
    $("nutritionMessage").textContent =
      "Nutrition history could not be loaded. Please refresh and try again.";
  }
  renderGrowthChart(records?.growth || []);

  $("detailPanel").hidden = false;
  $("detailTitle").textContent = child.name;
  $("detailSubtitle").textContent = `Child ID: ${child.childId}`;

  $("measurementForm").elements.childId.value = child.childId;
  $("measurementForm").elements.date.value = today();
  $("measurementForm").elements.date.max = today();

  $("measurementMessage").textContent = "";
  $("followupMessage").textContent = "";

  $("historyBody").innerHTML = [...(records?.growth || [])]
    .sort((a, b) =>
      String(b.measurementDate).localeCompare(
        String(a.measurementDate)
      )
    )
    .map((item) => `
      <tr>
        <td>${escapeHTML(item.measurementDate)}</td>
        <td>${escapeHTML(item.heightCm)} cm</td>
        <td>${escapeHTML(item.weightKg)} kg</td>
        <td>${escapeHTML(item.notes || "—")}</td>
      </tr>`
    ).join("");

  if (records?.growthError) {
    $("measurementMessage").textContent =
      "Growth history could not be loaded. Please refresh and try again.";
  }

  const followups = records?.followups || [];
  const latestFollowup = followups[followups.length - 1];

  $("followupNote").value = latestFollowup?.note || "";

  if (records?.followupError) {
    $("followupMessage").textContent =
      "Follow-up records could not be loaded.";
  }

  $("detailPanel").scrollIntoView({
    behavior: "smooth",
    block: "start"
  });
}

$("showFormBtn").addEventListener("click", () => {
  $("registrationPanel").hidden = false;
  $("registrationPanel").scrollIntoView({ behavior: "smooth" });
});

$("closeFormBtn").addEventListener("click", () => {
  $("registrationPanel").hidden = true;
});

$("closeDetailBtn").addEventListener("click", () => {
  $("detailPanel").hidden = true;
});

$("searchInput").addEventListener("input", renderDashboard);
$("riskFilter").addEventListener("change", renderDashboard);

$("childTableBody").addEventListener("click", (event) => {
  const button = event.target.closest("[data-action='view']");

  if (button) {
    openChildDetails(button.dataset.id);
  }
});

$("childForm").addEventListener("submit", async (event) => {
  event.preventDefault();

  const form = event.currentTarget;
  if (!form.reportValidity()) return;

  const data = new FormData(form);

  const childData = {
    name: String(data.get("name")).trim(),
    dateOfBirth: String(data.get("dob")),
    guardianName: String(data.get("guardian")).trim(),
    guardianMobile: String(data.get("mobile")).trim(),
    gender: String(data.get("gender")),
    centreName: String(data.get("centre")).trim()
  };

  $("formMessage").textContent = "Saving child profile...";

  try {
    const created = await apiRequest("/children", {
      method: "POST",
      body: JSON.stringify(childData)
    });

    if (!created?.childId) {
      throw new Error(
        "The server did not return a Child ID. Check the API response."
      );
    }

    form.reset();

    $("formMessage").textContent =
      `Child registered successfully. Child ID: ${created.childId}`;

    await renderDashboard();
    openChildDetails(created.childId);
  } catch (error) {
    $("formMessage").textContent =
      `Could not register child: ${error.message}`;
  }
});

$("measurementForm").addEventListener("submit", async (event) => {
  event.preventDefault();

  const form = event.currentTarget;
  if (!form.reportValidity()) return;

  const data = new FormData(form);
  const childId = String(data.get("childId"));
  const date = String(data.get("date"));

  if (date > today()) {
    $("measurementMessage").textContent =
      "Measurement date cannot be in the future.";
    return;
  }

  const measurement = {
    measurementDate: date,
    heightCm: Number(data.get("height")),
    weightKg: Number(data.get("weight")),
    notes: String(data.get("notes")).trim() || null
  };

  $("measurementMessage").textContent = "Saving measurement...";

  try {
    await apiRequest(
      `/children/${encodeURIComponent(childId)}/growth`,
      {
        method: "POST",
        body: JSON.stringify(measurement)
      }
    );

    $("measurementMessage").textContent =
      "Measurement saved to the database.";

    await renderDashboard();
    openChildDetails(childId);
    $("measurementMessage").textContent =
      "Measurement saved to the database.";
  } catch (error) {
    $("measurementMessage").textContent =
      `Could not save measurement: ${error.message}`;
  }
});

$("saveFollowupBtn").addEventListener("click", async () => {
  const session = getSession();
  const childId = $("measurementForm").elements.childId.value;
  const note = $("followupNote").value.trim();

  if (!session?.userId || session.role !== "WORKER") {
    $("followupMessage").textContent =
      "Worker session is missing. Please log in again.";
    return;
  }

  if (!note) {
    $("followupMessage").textContent =
      "Enter a follow-up note first.";
    return;
  }

  const followupData = {
    note,
    assignedWorkerId: session.userId,
    status: "OPEN"
  };

  $("followupMessage").textContent = "Saving follow-up...";

  try {
    await apiRequest(
      `/children/${encodeURIComponent(childId)}/followups`,
      {
        method: "POST",
        body: JSON.stringify(followupData)
      }
    );

    $("followupMessage").textContent =
      "Follow-up created in the database. No SMS was sent.";

    await renderDashboard();
    openChildDetails(childId);
    $("followupNote").value = note;
    $("followupMessage").textContent =
      "Follow-up created in the database. No SMS was sent.";
  } catch (error) {
    $("followupMessage").textContent =
      `Could not save follow-up: ${error.message}`;
  }
});


$("nutritionForm").addEventListener("submit", async (event) => {
  event.preventDefault();

  const form = event.currentTarget;
  if (!form.reportValidity()) return;

  const data = new FormData(form);
  const childId = String(data.get("childId") || "");
  const recordDate = String(data.get("recordDate") || "");

  if (!childId) {
    $("nutritionMessage").textContent =
      "Please select a child first.";
    return;
  }

  if (recordDate > today()) {
    $("nutritionMessage").textContent =
      "Record date cannot be in the future.";
    return;
  }

  const optionalNumber = (name) => {
    const value = String(data.get(name) ?? "").trim();
    return value === "" ? null : Number(value);
  };

  const nutritionData = {
    recordDate,
    foodConsumed: String(data.get("foodConsumed") || "").trim() || null,
    caloriesConsumedKcal: optionalNumber("caloriesConsumedKcal"),
    caloriesRequiredKcal: optionalNumber("caloriesRequiredKcal"),
    proteinConsumedG: optionalNumber("proteinConsumedG"),
    proteinRequiredG: optionalNumber("proteinRequiredG"),
    carbsConsumedG: optionalNumber("carbsConsumedG"),
    carbsRequiredG: optionalNumber("carbsRequiredG"),
    notes: String(data.get("notes") || "").trim() || null
  };

  for (const [key, value] of Object.entries(nutritionData)) {
    if (value !== null && typeof value === "number" &&
        (!Number.isFinite(value) || value < 0)) {
      $("nutritionMessage").textContent =
        "Nutrition quantities must be valid non-negative numbers.";
      return;
    }
  }

  $("nutritionMessage").textContent = "Saving nutrition record...";

  try {
    await apiRequest(
      `/children/${encodeURIComponent(childId)}/nutrition`,
      {
        method: "POST",
        body: JSON.stringify(nutritionData)
      }
    );

    await loadChildRecords(childId);
    renderNutritionHistory(
      recordsCache.get(childId)?.nutrition || []
    );

    renderNutritionSummary(
     recordsCache.get(childId)?.nutrition || []
    );

    form.reset();
    form.elements.childId.value = childId;
    form.elements.recordDate.value = today();
    form.elements.recordDate.max = today();

    $("nutritionMessage").textContent =
      "Nutrition record saved successfully.";

  } catch (error) {
    $("nutritionMessage").textContent =
      `Could not save nutrition record: ${error.message}`;
  }
});


// Clear the local login session when the worker logs out.
document.querySelector(".logout")?.addEventListener("click", () => {
  sessionStorage.removeItem("poshancare_session");
});

requireWorkerSession();
if (getSession()?.role === "WORKER") {
  renderDashboard();
}
