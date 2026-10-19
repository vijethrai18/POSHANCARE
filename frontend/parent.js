
const API_BASE_URL = "http://localhost:8080/api";

const $ = (id) => document.getElementById(id);

let parentGrowthChart = null;
let currentChildId = null;

function getSession() {
  try {
    return JSON.parse(
      sessionStorage.getItem("poshancare_session") || "null"
    );
  } catch {
    return null;
  }
}

function logout() {
  sessionStorage.removeItem("poshancare_session");
  window.location.href = "login.html";
}

function formatDate(value) {
  if (!value) return "—";

  const date = new Date(`${String(value).slice(0, 10)}T00:00:00`);

  if (Number.isNaN(date.getTime())) {
    return String(value);
  }

  return date.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  });
}

function showMessage(message, targetId = "pageMessage") {
  const element = $(targetId);
  if (element) element.textContent = message || "";
}

async function apiGet(path) {
  const session = getSession();

  if (!session?.token) {
    throw new Error("Your session has expired. Please log in again.");
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${session.token}`,
      Accept: "application/json"
    }
  });

  if (response.status === 401) {
    logout();
    throw new Error("Your session has expired. Please log in again.");
  }

  if (response.status === 403) {
    throw new Error("You are not authorised to view these records.");
  }

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(
      errorText || `Unable to load records (HTTP ${response.status}).`
    );
  }

  return response.json();
}

function latestMeasurement(records) {
  if (!Array.isArray(records) || records.length === 0) {
    return null;
  }

  return [...records].sort((a, b) =>
    String(b.measurementDate || "").localeCompare(
      String(a.measurementDate || "")
    )
  )[0];
}

function renderGrowthHistory(records) {
  const body = $("growthBody");
  const noMeasurements = $("noMeasurements");

  if (!body || !noMeasurements) return;

  body.replaceChildren();

  if (!Array.isArray(records) || records.length === 0) {
    noMeasurements.hidden = false;

    const row = document.createElement("tr");
    const cell = document.createElement("td");

    cell.colSpan = 4;
    cell.className = "table-message";
    cell.textContent = "No growth measurements have been recorded yet.";

    row.appendChild(cell);
    body.appendChild(row);
    return;
  }

  noMeasurements.hidden = true;

  const history = [...records].sort((a, b) =>
    String(b.measurementDate || "").localeCompare(
      String(a.measurementDate || "")
    )
  );

  history.forEach((record) => {
    const row = document.createElement("tr");

    const values = [
      formatDate(record.measurementDate),
      record.heightCm == null ? "—" : `${record.heightCm} cm`,
      record.weightKg == null ? "—" : `${record.weightKg} kg`,
      record.notes || "—"
    ];

    values.forEach((value) => {
      const cell = document.createElement("td");
      cell.textContent = value;
      row.appendChild(cell);
    });

    body.appendChild(row);
  });
}

function renderParentGrowthChart(records) {
  const canvas = $("parentGrowthChart");
  const message = $("parentChartMessage");

  if (!canvas || !message) return;

  if (parentGrowthChart) {
    parentGrowthChart.destroy();
    parentGrowthChart = null;
  }

  if (typeof Chart === "undefined") {
    canvas.hidden = true;
    message.textContent =
      "The chart library could not load. Check your internet connection.";
    return;
  }

  const history = [...records].sort((a, b) =>
    String(a.measurementDate || "").localeCompare(
      String(b.measurementDate || "")
    )
  );

  if (history.length === 0) {
    canvas.hidden = true;
    message.textContent =
      "The growth chart will appear when measurements are recorded.";
    return;
  }

  canvas.hidden = false;
  message.textContent = "";

  parentGrowthChart = new Chart(canvas, {
    type: "line",

    data: {
      labels: history.map((record) =>
        formatDate(record.measurementDate)
      ),

      datasets: [
        {
          label: "Height (cm)",
          data: history.map((record) =>
            record.heightCm == null ? null : Number(record.heightCm)
          ),
          borderColor: "#21865b",
          backgroundColor: "rgba(33, 134, 91, 0.12)",
          borderWidth: 2,
          pointRadius: 4,
          pointHoverRadius: 6,
          tension: 0.25,
          yAxisID: "heightAxis",
          spanGaps: false
        },
        {
          label: "Weight (kg)",
          data: history.map((record) =>
            record.weightKg == null ? null : Number(record.weightKg)
          ),
          borderColor: "#e09b36",
          backgroundColor: "rgba(224, 155, 54, 0.12)",
          borderWidth: 2,
          pointRadius: 4,
          pointHoverRadius: 6,
          tension: 0.25,
          yAxisID: "weightAxis",
          spanGaps: false
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
          callbacks: {
            label(context) {
              const value = context.parsed.y;
              if (value == null) {
                return `${context.dataset.label}: —`;
              }
              return `${context.dataset.label}: ${value}`;
            }
          }
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
          beginAtZero: false,
          title: {
            display: true,
            text: "Height (cm)"
          }
        },

        weightAxis: {
          type: "linear",
          position: "right",
          beginAtZero: false,
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

function renderFollowups(records) {
  const container = $("followupContent");
  if (!container) return;

  container.replaceChildren();

  if (!Array.isArray(records) || records.length === 0) {
    const empty = document.createElement("p");
    empty.className = "muted";
    empty.textContent = "There are no follow-up records yet.";
    container.appendChild(empty);
    return;
  }

  const sortedRecords = [...records].sort((a, b) =>
    String(b.dueDate || "").localeCompare(String(a.dueDate || ""))
  );

  sortedRecords.forEach((record) => {
    const item = document.createElement("article");
    item.className = "followup-item";

    const status = document.createElement("span");
    status.className = "status-pill";
    status.textContent = String(record.status || "OPEN").replaceAll("_", " ");

    const note = document.createElement("p");
    note.textContent = record.note || "No note provided.";

    const meta = document.createElement("div");
    meta.className = "followup-meta";

    const dueDate = document.createElement("span");
    dueDate.textContent = `Due date: ${
      record.dueDate ? formatDate(record.dueDate) : "Not specified"
    }`;

    meta.appendChild(dueDate);
    item.append(status, note, meta);
    container.appendChild(item);
  });
}

/* NEW: Render nutrition records from the backend */
function renderNutritionRecords(records) {
  const container = $("nutritionContent");
  if (!container) return;

  container.replaceChildren();

  if (!Array.isArray(records) || records.length === 0) {
    const empty = document.createElement("p");
    empty.className = "muted";
    empty.textContent =
      "No nutrition records have been recorded yet.";
    container.appendChild(empty);
    return;
  }

  const sortedRecords = [...records].sort((a, b) =>
    String(b.recordDate || "").localeCompare(
      String(a.recordDate || "")
    )
  );

  const fields = [
    ["foodConsumed", "Food consumed", ""],
    ["caloriesConsumedKcal", "Calories consumed", " kcal"],
    ["caloriesRequiredKcal", "Calories required", " kcal"],
    ["proteinConsumedG", "Protein consumed", " g"],
    ["proteinRequiredG", "Protein required", " g"],
    ["carbsConsumedG", "Carbohydrates consumed", " g"],
    ["carbsRequiredG", "Carbohydrates required", " g"],
    ["notes", "Notes", ""]
  ];

  sortedRecords.forEach((record) => {
    const item = document.createElement("article");
    item.className = "nutrition-item";

    const heading = document.createElement("h4");
    heading.textContent = `Nutrition record — ${formatDate(record.recordDate)}`;
    item.appendChild(heading);

    fields.forEach(([key, label, unit]) => {
      const value = record[key];

      if (value === null || value === undefined || value === "") {
        return;
      }

      const line = document.createElement("p");
      line.className = "nutrition-detail";
      line.textContent = `${label}: ${value}${unit}`;
      item.appendChild(line);
    });

    container.appendChild(item);
  });
}


/* Render the latest nutrition summary for the parent */
function renderParentNutritionSummary(records) {
  const container = $("parentNutritionSummary");
  if (!container) return;

  container.replaceChildren();

  if (!Array.isArray(records) || records.length === 0) {
    const empty = document.createElement("p");
    empty.className = "muted";
    empty.textContent = "No nutrition records are available yet.";
    container.appendChild(empty);
    return;
  }

  const latest = [...records].sort((a, b) =>
    String(b.recordDate || "").localeCompare(
      String(a.recordDate || "")
    )
  )[0];

  const date = document.createElement("p");
  date.className = "parent-nutrition-date";
  date.textContent =
    `Latest record: ${formatDate(latest.recordDate)}`;
  container.appendChild(date);

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
  grid.className = "parent-nutrition-grid";

  metrics.forEach((metric) => {
    const card = document.createElement("article");
    card.className = "parent-nutrition-card";

    const heading = document.createElement("h4");
    heading.textContent = metric.label;

    const consumed = document.createElement("p");
    consumed.className = "parent-nutrition-value";
    consumed.textContent =
      metric.consumed == null
        ? "Not recorded"
        : `${metric.consumed} ${metric.unit}`;

    const required = document.createElement("p");
    required.className = "parent-nutrition-detail";
    required.textContent =
      metric.required == null
        ? "Required: Not recorded"
        : `Required: ${metric.required} ${metric.unit}`;

    const comparison = document.createElement("p");
    comparison.className = "parent-nutrition-detail";

    if (
      metric.consumed == null ||
      metric.required == null ||
      metric.consumed === "" ||
      metric.required === "" ||
      !Number.isFinite(Number(metric.consumed)) ||
      !Number.isFinite(Number(metric.required))
    ) {
      comparison.textContent = "Comparison unavailable";
    } else {
      const difference =
        Number(metric.consumed) - Number(metric.required);

      comparison.textContent =
        `Difference: ${difference > 0 ? "+" : ""}${Number(difference.toFixed(2))} ${metric.unit}`;
    }

    card.append(heading, consumed, required, comparison);
    grid.appendChild(card);
  });

  container.appendChild(grid);

  if (latest.foodConsumed) {
    const food = document.createElement("p");
    food.className = "parent-nutrition-extra";
    food.textContent = `Food consumed: ${latest.foodConsumed}`;
    container.appendChild(food);
  }

  if (latest.notes) {
    const notes = document.createElement("p");
    notes.className = "parent-nutrition-extra";
    notes.textContent = `Notes: ${latest.notes}`;
    container.appendChild(notes);
  }
}


function showProfile(child, growthRecords, followups, nutritionRecords) {
  $("childName").textContent = child.name || "Child profile";

  const details = [
    child.childId ? `Child ID: ${child.childId}` : "",
    child.dateOfBirth ? `Date of birth: ${formatDate(child.dateOfBirth)}` : "",
    child.gender ? `Gender: ${child.gender}` : "",
    child.centreName ? `Centre: ${child.centreName}` : ""
  ].filter(Boolean);

  $("childMeta").textContent = details.join(" · ");

  // Do not invent a medical risk classification.
  $("riskStatus").textContent = "Not assessed";

  const latest = latestMeasurement(growthRecords);

  $("childHeight").textContent =
    latest?.heightCm != null ? `${latest.heightCm} cm` : "—";

  $("childWeight").textContent =
    latest?.weightKg != null ? `${latest.weightKg} kg` : "—";

  $("measurementDate").textContent =
    latest?.measurementDate ? formatDate(latest.measurementDate) : "—";

  renderGrowthHistory(growthRecords);
  renderParentGrowthChart(growthRecords);
  renderFollowups(followups);
  renderNutritionRecords(nutritionRecords);
  renderParentNutritionSummary(nutritionRecords);

  $("profilePanel").hidden = false;
  $("lookupPanel").hidden = true;
  showMessage("");
}

async function loadParentDashboard() {
  const session = getSession();

  if (!session?.token) {
    window.location.href = "login.html";
    return;
  }

  if (session.role !== "PARENT") {
    showMessage("Please log in with a parent account.");
    return;
  }

  if (!session.childId) {
    showMessage(
      "No Child ID is linked to this login. Please sign in again using your Child ID."
    );
    $("lookupPanel").hidden = false;
    $("profilePanel").hidden = true;
    return;
  }

  currentChildId = session.childId;

  $("profilePanel").hidden = true;
  $("lookupPanel").hidden = true;
  showMessage("Loading your child's records...");

  try {
    const encodedChildId = encodeURIComponent(currentChildId);

    /*
     * Fetch nutrition records together with the existing records.
     * Nutrition failures are handled separately so that the growth
     * chart and follow-ups can still load.
     */
    const [child, growthRecords, followups] = await Promise.all([
      apiGet(`/children/${encodedChildId}`),
      apiGet(`/children/${encodedChildId}/growth`),
      apiGet(`/children/${encodedChildId}/followups`)
    ]);

    let nutritionRecords = [];

    try {
      const result = await apiGet(
        `/children/${encodedChildId}/nutrition`
      );
      nutritionRecords = Array.isArray(result) ? result : [];
    } catch (nutritionError) {
      console.error("Nutrition records could not be loaded:", nutritionError);
      showMessage(
        "Your profile loaded, but nutrition records could not be loaded. Please check the nutrition API access.",
        "nutritionMessage"
      );
    }

    if (child.childId !== currentChildId) {
      throw new Error("The returned Child ID does not match your account.");
    }

    showProfile(
      child,
      Array.isArray(growthRecords) ? growthRecords : [],
      Array.isArray(followups) ? followups : [],
      nutritionRecords
    );
  } catch (error) {
    console.error("Parent dashboard error:", error);

    $("profilePanel").hidden = true;
    showMessage(
      error.message ||
      "Unable to load the records. Check that the backend is running."
    );
  }
}

// Language selection is saved for later use.
const languageSelect = $("parentLanguage");
const savedLanguage = localStorage.getItem("poshancare_demo_language");

if (savedLanguage && languageSelect) {
  languageSelect.value = savedLanguage;
}

if (languageSelect) {
  languageSelect.addEventListener("change", () => {
    localStorage.setItem(
      "poshancare_demo_language",
      languageSelect.value
    );
  });
}

const logoutButton = $("logoutBtn");
if (logoutButton) {
  logoutButton.addEventListener("click", logout);
}

const changeProfileButton = $("changeProfileBtn");
if (changeProfileButton) {
  changeProfileButton.addEventListener("click", loadParentDashboard);
}

const reloadProfileButton = $("reloadProfileBtn");
if (reloadProfileButton) {
  reloadProfileButton.addEventListener("click", loadParentDashboard);
}

// Load the logged-in parent's linked child automatically.
loadParentDashboard();
