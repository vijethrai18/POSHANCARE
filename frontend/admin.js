
"use strict";

const API_BASE = "https://poshancare.onrender.com/api";

const loginSection = document.getElementById("loginSection");
const dashboardSection = document.getElementById("dashboardSection");
const loginForm = document.getElementById("loginForm");
const loginButton = document.getElementById("loginButton");
const loginMessage = document.getElementById("loginMessage");
const dashboardMessage = document.getElementById("dashboardMessage");
const applicationsList = document.getElementById("applicationsList");

const adminName = document.getElementById("adminName");
const pendingCount = document.getElementById("pendingCount");
const displayedCount = document.getElementById("displayedCount");

const refreshButton = document.getElementById("refreshButton");
const logoutButton = document.getElementById("logoutButton");

let adminToken = sessionStorage.getItem("poshancareAdminToken");
let adminDetails = null;
let isLoadingApplications = false;

// Display a message safely.
function showMessage(element, message, type = "") {
  element.textContent = message;
  element.className = "message";

  if (message && type) {
    element.classList.add(type);
  }
}

// Make authenticated requests to the backend.
async function apiRequest(path, options = {}) {
  const headers = {
    ...(options.headers || {})
  };

  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  if (adminToken) {
    headers["Authorization"] = `Bearer ${adminToken}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  });

  const responseText = await response.text();
  let data = null;

  if (responseText) {
    try {
      data = JSON.parse(responseText);
    } catch {
      data = { message: responseText };
    }
  }

  if (!response.ok) {
    const error = new Error(
      data?.message ||
      data?.error ||
      `Request failed with HTTP ${response.status}`
    );

    error.status = response.status;
    throw error;
  }

  return data;
}

// Start administrator login.
loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();

  const mobile = document.getElementById("mobile").value.trim();
  const password = document.getElementById("password").value;

  showMessage(loginMessage, "");

  loginButton.disabled = true;
  loginButton.textContent = "Signing in...";

  try {
    const result = await apiRequest("/auth/admin-login", {
      method: "POST",
      body: JSON.stringify({ mobile, password })
    });

    if (!result?.token) {
      throw new Error(
        "The server did not return a login token. Check the admin-login API response."
      );
    }

    if (result.role && result.role !== "ADMIN") {
      throw new Error("This account is not an administrator.");
    }

    adminToken = result.token;

    // Keep the token for this browser tab's session.
    sessionStorage.setItem("poshancareAdminToken", adminToken);

    adminDetails = result;
    openDashboard();
    await loadApplications();
  } catch (error) {
    showMessage(
      loginMessage,
      error.message || "Login failed. Please try again.",
      "error"
    );
  } finally {
    loginButton.disabled = false;
    loginButton.textContent = "Sign in";
  }
});

// Display the admin dashboard.
function openDashboard() {
  loginSection.hidden = true;
  dashboardSection.hidden = false;

  adminName.textContent =
    adminDetails?.fullName ||
    adminDetails?.name ||
    "Administrator";
}

// Load pending worker registrations.
async function loadApplications() {
  if (!adminToken || isLoadingApplications) {
    return;
  }

  isLoadingApplications = true;
  refreshButton.disabled = true;
  refreshButton.textContent = "Loading...";

  showMessage(dashboardMessage, "");
  applicationsList.replaceChildren();

  const loading = document.createElement("div");
  loading.className = "empty-state";
  loading.textContent = "Loading worker registrations...";
  applicationsList.appendChild(loading);

  try {
    const result = await apiRequest(
      "/admin/worker-registrations?status=PENDING"
    );

    // Support either a direct array or a common wrapped response.
    let registrations;

    if (Array.isArray(result)) {
      registrations = result;
    } else if (Array.isArray(result?.registrations)) {
      registrations = result.registrations;
    } else if (Array.isArray(result?.data)) {
      registrations = result.data;
    } else if (Array.isArray(result?.content)) {
      registrations = result.content;
    } else {
      throw new Error(
        "Unexpected response format from the registrations API. Check the response in Postman."
      );
    }

    renderApplications(registrations);
    showMessage(
      dashboardMessage,
      registrations.length
        ? ""
        : "There are no pending worker applications right now."
    );
  } catch (error) {
    applicationsList.replaceChildren();

    const empty = document.createElement("div");
    empty.className = "empty-state";
    empty.textContent =
      error.status === 401 || error.status === 403
        ? "Your session may have expired or access was denied. Please sign in again."
        : `Could not load applications: ${error.message}`;

    applicationsList.appendChild(empty);

    if (error.status === 401 || error.status === 403) {
      showMessage(
        dashboardMessage,
        "Please sign in again with an administrator account.",
        "error"
      );
    }
  } finally {
    isLoadingApplications = false;
    refreshButton.disabled = false;
    refreshButton.textContent = "↻ Refresh";
  }
}

// Build application cards using textContent to avoid inserting HTML from the API.
function renderApplications(registrations) {
  applicationsList.replaceChildren();

  pendingCount.textContent = String(registrations.length);
  displayedCount.textContent = String(registrations.length);

  if (registrations.length === 0) {
    const empty = document.createElement("div");
    empty.className = "empty-state";
    empty.textContent =
      "All caught up! No worker registrations are awaiting approval.";
    applicationsList.appendChild(empty);
    return;
  }

  registrations.forEach((worker) => {
    const workerId =
      worker.id ??
      worker.userId ??
      worker.workerId ??
      "";

    const fullName =
      worker.fullName ??
      worker.name ??
      "Name not provided";

    const mobile =
      worker.mobile ??
      worker.mobileNumber ??
      "Not provided";

    const centreName =
      worker.centreName ??
      worker.anganwadiCentre ??
      worker.centerName ??
      "Not provided";

    const language =
      worker.preferredLanguage ??
      "Not provided";

    const card = document.createElement("article");
    card.className = "application-card";

    const top = document.createElement("div");
    top.className = "application-top";

    const headingGroup = document.createElement("div");
    const name = document.createElement("h3");
    name.textContent = fullName;

    const id = document.createElement("div");
    id.className = "application-id";
    id.textContent = `Worker ID: ${workerId || "Not provided"}`;

    headingGroup.append(name, id);

    const status = document.createElement("span");
    status.className = "status-pill";
    status.textContent = worker.status || "PENDING";

    top.append(headingGroup, status);

    const details = document.createElement("div");
    details.className = "application-details";

    details.append(
      createDetail("Mobile number", mobile),
      createDetail("Anganwadi centre", centreName),
      createDetail("Preferred language", language),
      createDetail("Role", worker.role || "WORKER")
    );

    const actions = document.createElement("div");
    actions.className = "application-actions";

    const rejectButton = document.createElement("button");
    rejectButton.className = "action-button reject";
    rejectButton.type = "button";
    rejectButton.textContent = "Reject";
    rejectButton.disabled = !workerId;

    const approveButton = document.createElement("button");
    approveButton.className = "action-button approve";
    approveButton.type = "button";
    approveButton.textContent = "Approve";
    approveButton.disabled = !workerId;

    rejectButton.addEventListener("click", () => {
      updateRegistration(workerId, "reject", fullName, card);
    });

    approveButton.addEventListener("click", () => {
      updateRegistration(workerId, "approve", fullName, card);
    });

    actions.append(rejectButton, approveButton);
    card.append(top, details, actions);
    applicationsList.appendChild(card);
  });
}

function createDetail(labelText, valueText) {
  const container = document.createElement("div");
  container.className = "detail-item";

  const label = document.createElement("span");
  label.textContent = labelText;

  const value = document.createElement("strong");
  value.textContent = String(valueText);

  container.append(label, value);
  return container;
}

// Approve or reject a registration through the backend.
async function updateRegistration(workerId, action, workerName, card) {
  if (!workerId) {
    showMessage(
      dashboardMessage,
      "This application has no worker ID. Check the API response.",
      "error"
    );
    return;
  }

  const isApprove = action === "approve";
  const confirmation = isApprove
    ? `Approve ${workerName}'s worker registration?`
    : `Reject ${workerName}'s worker registration?`;

  if (!window.confirm(confirmation)) {
    return;
  }

  const buttons = card.querySelectorAll("button");
  buttons.forEach((button) => {
    button.disabled = true;
  });

  try {
    await apiRequest(
      `/admin/worker-registrations/${encodeURIComponent(workerId)}/${action}`,
      { method: "PATCH" }
    );

    showMessage(
      dashboardMessage,
      `${workerName}'s registration was ${isApprove ? "approved" : "rejected"}.`,
      "success"
    );

    await loadApplications();
  } catch (error) {
    showMessage(
      dashboardMessage,
      `Could not ${action} this registration: ${error.message}`,
      "error"
    );

    buttons.forEach((button) => {
      button.disabled = false;
    });
  }
}

// Refresh the pending list.
refreshButton.addEventListener("click", loadApplications);

// Sign out.
logoutButton.addEventListener("click", () => {
  sessionStorage.removeItem("poshancareAdminToken");

  adminToken = null;
  adminDetails = null;

  loginForm.reset();
  dashboardSection.hidden = true;
  loginSection.hidden = false;

  pendingCount.textContent = "—";
  displayedCount.textContent = "—";

  showMessage(loginMessage, "You have signed out.", "success");
  showMessage(dashboardMessage, "");
});

// Start on the login screen.
// A saved token alone is not treated as proof of a valid session.
loginSection.hidden = false;
dashboardSection.hidden = true;
