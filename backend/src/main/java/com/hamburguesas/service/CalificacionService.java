package com.hamburguesas.service;

import com.hamburguesas.dto.CalificacionRequest;
import com.hamburguesas.dto.CalificacionResponse;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.Calificacion;
import com.hamburguesas.model.Hamburgueseria;
import com.hamburguesas.model.Usuario;
import com.hamburguesas.repository.CalificacionRepository;
import com.hamburguesas.repository.HamburgueseriaRepository;
import com.hamburguesas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalificacionService {

    private final CalificacionRepository calificacionRepository;
    private final HamburgueseriaRepository hamburgueseriaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public CalificacionResponse calificar(Long usuarioId, Long hamburgueseriaId, CalificacionRequest request) {
        if (calificacionRepository.findByUsuario_IdAndHamburgueseria_Id(usuarioId, hamburgueseriaId).isPresent()) {
            throw new ConflictException("Ya calificaste esta hamburgueseria. Editala en vez de crear otra.");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Hamburgueseria hamburgueseria = hamburgueseriaRepository.findById(hamburgueseriaId)
            .orElseThrow(() -> new ResourceNotFoundException("Hamburgueseria no encontrada"));

        Calificacion calificacion = Calificacion.builder()
            .usuario(usuario)
            .hamburgueseria(hamburgueseria)
            .puntaje(request.puntaje())
            .comentario(request.comentario())
            .build();

        calificacion = calificacionRepository.save(calificacion);
        return toResponse(calificacion);
    }

    @Transactional
    public CalificacionResponse editar(Long usuarioId, Long hamburgueseriaId, CalificacionRequest request) {
        Calificacion calificacion = calificacionRepository
            .findByUsuario_IdAndHamburgueseria_Id(usuarioId, hamburgueseriaId)
            .orElseThrow(() -> new ResourceNotFoundException("No calificaste esta hamburgueseria todavia"));

        calificacion.setPuntaje(request.puntaje());
        calificacion.setComentario(request.comentario());
        return toResponse(calificacion);
    }

    public Page<CalificacionResponse> listar(Long hamburgueseriaId, Pageable pageable) {
        return calificacionRepository
            .findByHamburgueseria_IdOrderByFechaDesc(hamburgueseriaId, pageable)
            .map(this::toResponse);
    }

    private CalificacionResponse toResponse(Calificacion c) {
        return new CalificacionResponse(
            c.getId(), c.getUsuario().getId(), c.getUsuario().getNombre(),
            c.getPuntaje(), c.getComentario(), c.getFecha()
        );
    }
}
