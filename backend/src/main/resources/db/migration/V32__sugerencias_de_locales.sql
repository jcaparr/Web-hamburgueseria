-- Locales que la gente sugiere porque no están en Burgómetro (#257).
--
-- Juan las revisa a mano en la base y, si corresponde, agrega el local con la skill
-- agregar-local. Guardar una sugerencia no llama a Google: es solo texto.
--
-- El email se guarda aparte del de la cuenta porque la persona lo puede cambiar al
-- sugerir. Si se borra la cuenta, se van sus sugerencias, como el resto de sus datos.
create table sugerencias_de_locales (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    nombre varchar(120) not null,
    direccion varchar(500) not null,
    email varchar(254) not null,
    comentario varchar(1000),
    created_at timestamp(6) with time zone not null
);

-- El tope de 5 por día cuenta las de cada persona desde una fecha.
create index sugerencias_de_locales_user_created on sugerencias_de_locales (user_id, created_at);
