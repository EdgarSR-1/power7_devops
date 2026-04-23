/*
## MyToDoReact version 2.0.
##
## Copyright (c) 2022 Oracle, Inc.
## Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
*/
/*
 * Improved user view with task cards, status badges, priority indicators,
 * and a Details modal that shows full task information.
 */
import React, { useState, useEffect } from 'react';
import { Button, CircularProgress, Dialog, DialogTitle, DialogContent, DialogActions, Chip } from '@mui/material';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import Moment from 'react-moment';

const API_TASKS = '/api/tasks';

const STATUS_LABELS = {
  pending: 'Pendiente',
  in_progress: 'En Progreso',
  completed: 'Completado',
};

const PRIORITY_LABELS = {
  low: 'Baja',
  medium: 'Media',
  high: 'Alta',
};

const STATUS_ORDER = ['pending', 'in_progress', 'completed'];

function StatusBadge({ status }) {
  return (
    <span className={`status-badge status-${status}`}>
      {STATUS_LABELS[status] || status}
    </span>
  );
}

function PriorityBadge({ priority }) {
  return (
    <span className={`priority-badge priority-${priority}`}>
      {PRIORITY_LABELS[priority] || priority}
    </span>
  );
}

function TaskDetailRow({ label, value }) {
  if (!value) return null;
  return (
    <div className="detail-row">
      <span className="detail-label">{label}</span>
      <span className="detail-value">{value}</span>
    </div>
  );
}

function TaskDetailsModal({ task, open, onClose }) {
  if (!task) return null;
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth
      PaperProps={{ className: 'details-dialog' }}>
      <DialogTitle className="details-dialog-title">
        Detalles de la Tarea
      </DialogTitle>
      <DialogContent className="details-dialog-content">
        <h3 className="details-task-title">{task.title}</h3>
        <div className="details-badges">
          <StatusBadge status={task.status} />
          {task.priority && <PriorityBadge priority={task.priority} />}
        </div>
        {task.description && (
          <div className="details-description">
            <span className="detail-label">Descripción</span>
            <p>{task.description}</p>
          </div>
        )}
        <div className="details-grid">
          <TaskDetailRow label="Asignado a" value={task.assigneeName} />
          <TaskDetailRow label="Sprint" value={task.sprintName} />
          <TaskDetailRow label="Grupo" value={task.groupName} />
          <TaskDetailRow label="Lista" value={task.todoListName} />
          {task.dueDate && (
            <TaskDetailRow
              label="Fecha límite"
              value={<Moment format="DD/MM/YYYY">{task.dueDate}</Moment>}
            />
          )}
          {task.startDate && (
            <TaskDetailRow
              label="Inicio"
              value={<Moment format="DD/MM/YYYY">{task.startDate}</Moment>}
            />
          )}
          {task.endDate && (
            <TaskDetailRow
              label="Fin"
              value={<Moment format="DD/MM/YYYY">{task.endDate}</Moment>}
            />
          )}
          {task.createdAt && (
            <TaskDetailRow
              label="Creado"
              value={<Moment format="DD/MM/YYYY HH:mm">{task.createdAt}</Moment>}
            />
          )}
        </div>
      </DialogContent>
      <DialogActions className="details-dialog-actions">
        <Button onClick={onClose} variant="contained" className="CloseButton">
          Cerrar
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function TaskCard({ task, onDetails }) {
  return (
    <div className="task-card">
      <div className="task-card-header">
        <span className="task-card-title">{task.title}</span>
        <StatusBadge status={task.status} />
      </div>
      <div className="task-card-meta">
        {task.priority && <PriorityBadge priority={task.priority} />}
        {task.assigneeName && (
          <span className="task-assignee">👤 {task.assigneeName}</span>
        )}
        {task.dueDate && (
          <span className="task-due-date">
            📅 <Moment format="DD/MM/YYYY">{task.dueDate}</Moment>
          </span>
        )}
      </div>
      {task.description && (
        <p className="task-card-description">{task.description}</p>
      )}
      <div className="task-card-footer">
        <Button
          variant="outlined"
          className="DetailsButton"
          size="small"
          startIcon={<InfoOutlinedIcon />}
          onClick={() => onDetails(task)}
        >
          Detalles
        </Button>
      </div>
    </div>
  );
}

function TaskColumn({ title, tasks, onDetails }) {
  return (
    <div className="task-column">
      <div className="task-column-header">
        <span className="task-column-title">{title}</span>
        <Chip label={tasks.length} size="small" className="task-count-chip" />
      </div>
      <div className="task-column-body">
        {tasks.length === 0 ? (
          <p className="task-column-empty">Sin tareas</p>
        ) : (
          tasks.map(task => (
            <TaskCard key={task.id} task={task} onDetails={onDetails} />
          ))
        )}
      </div>
    </div>
  );
}

function App() {
  const [isLoading, setLoading] = useState(false);
  const [tasks, setTasks] = useState([]);
  const [error, setError] = useState(null);
  const [selectedTask, setSelectedTask] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);

  useEffect(() => {
    setLoading(true);
    fetch(API_TASKS)
      .then(response => {
        if (response.ok) {
          return response.json();
        }
        throw new Error('No se pudieron cargar las tareas.');
      })
      .then(
        result => {
          setLoading(false);
          setTasks(result);
        },
        err => {
          setLoading(false);
          setError(err);
        }
      );
  }, []);

  function handleOpenDetails(task) {
    setSelectedTask(task);
    setModalOpen(true);
  }

  function handleCloseDetails() {
    setModalOpen(false);
    setSelectedTask(null);
  }

  const tasksByStatus = STATUS_ORDER.reduce((acc, status) => {
    acc[status] = tasks.filter(t => {
      if (!t.status) {
        console.warn('Task missing status field:', t.id);
        return status === 'pending';
      }
      return t.status === status;
    });
    return acc;
  }, {});

  return (
    <div className="App">
      <h1 className="app-title">Panel de Tareas</h1>

      {error && (
        <p className="error-message">Error: {error.message}</p>
      )}

      {isLoading ? (
        <CircularProgress className="loading-spinner" />
      ) : (
        <div className="kanban-board">
          {STATUS_ORDER.map(status => (
            <TaskColumn
              key={status}
              title={STATUS_LABELS[status]}
              tasks={tasksByStatus[status]}
              onDetails={handleOpenDetails}
            />
          ))}
        </div>
      )}

      <TaskDetailsModal
        task={selectedTask}
        open={modalOpen}
        onClose={handleCloseDetails}
      />
    </div>
  );
}

export default App;
